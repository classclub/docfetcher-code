package net.sourceforge.docfetcher.model.parse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * A subclass of the PDFTextStripper class from PDFBox that provides a hook for
 * writing the annotations on each page, in the form of the abstract method
 * `writeAnnotations`.
 * <p>
 * Implementation note: Providing the hook is somewhat non-trivial, as during
 * processing PDFTextStripper skips pages without content, thus also skipping
 * pages without content, but with annotations. This class works around that
 * using reflection, ensuring that `writeAnnotations` is called even for empty
 * pages.
 * <p>
 * Before this class was introduced, the application used to ignore annotations
 * on empty pages. See the relevant bug report:
 * https://sourceforge.net/p/docfetcher/discussion/702424/thread/5880239730/
 * <p>
 * An alternative to the reflection-based approach used in this class is to
 * process annotations independently of PDFTextStripper. This could lead to
 * significant loss of performance on large PDF files, as the pages may have to
 * be traversed twice, once for the annotations and once for text extraction
 * using PDFTextStripper.
 * 
 * @author Tran Nam Quang
 */
abstract class PDFTextAndAnnotationStripper extends PDFTextStripper {
	
	private PDDocument doc;
	
	abstract protected boolean isCanceled();
	
	/*
	 * Note: This method is called for each page. If the page is empty, then it
	 * is called *before* the stripper's page counter is incremented, so the
	 * stripper's getCurrentPageNo method will report a value that is one less
	 * than the correct current page number.
	 */
	abstract protected void writeAnnotations(PDPage page);
	
	protected PDFTextAndAnnotationStripper() throws IOException {
    }
	
	@Override
	final protected void startDocument(PDDocument doc) {
		this.doc = doc;
	}
	
	@Override
	final public void processPages(PDPageTree pages) throws IOException {
		/* Wrap the given PDPageTree with a subclass overriding the iterator
		 * method. */
		final PDPageTree pagesWrapper = new PDPageTree(pages.getCOSObject()) {
			@Override
			public Iterator<PDPage> iterator() {
				final Iterator<PDPage> innerIt = super.iterator();
				return new Iterator<PDPage>() {
					public PDPage next() {
						final PDPage page = innerIt.next();
						if (!page.hasContents()) {
							writeAnnotations(page);
						}
						return page;
					}
					public boolean hasNext() {
						return innerIt.hasNext();
					}
				};
			}
		};
		
		// Use reflection to set a private field on our PDPageTree wrapper
		final Field docField;
		try {
			docField = pages.getClass().getDeclaredField("document");
			docField.setAccessible(true);
			docField.set(pagesWrapper, doc);
			super.processPages(pagesWrapper);
		} catch (NoSuchFieldException e) {
			super.processPages(pages);
		} catch (IllegalAccessException e) {
			super.processPages(pages);
		}
	}
	
	@Override
	final protected void endPage(PDPage page) {
		writeAnnotations(page);
		if (isCanceled()) {
			setEndPage(0);
		}
	}

}
