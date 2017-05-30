import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CypherFormFillPDF {
	private static PDDocument _pdfDocument;
	private static PDDocumentCatalog _docCatalog;
	private static PDAcroForm _acroForm;
	private static JSONObject _jsonObject;
	
	public static void main(String[] args) {
		
		if ( args.length != 3 ) {
			System.err.println("Usage: CypherFormFillPDF <input-pdf-path> <output-pdf-path> <json-data-path>");
			return;
		}
		
		String originalPdfPath 	= args[0];
		String targetPdfPath 	= args[1];
		String jsonDataPath 	= args[2];

		System.out.println( "Processing file: " + originalPdfPath );
		try {
			initialize(originalPdfPath, jsonDataPath);
			if (_pdfDocument == null) return;
			
			populateAndCopy(originalPdfPath, targetPdfPath);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("PDF Fill Complete");
	}

	private static void initialize(String originalPdf, String jsonDataPath) throws IOException {
		try {
			_pdfDocument = PDDocument.load(new File(originalPdf));			
		} catch (IOException e) {
			return;
		}
		System.out.println("Rotation: " + _pdfDocument.getPage(0).getRotation());
		
        _docCatalog = _pdfDocument.getDocumentCatalog();
        _acroForm = _docCatalog.getAcroForm();
        _jsonObject = parseJSON(jsonDataPath);
	}
	
	private static JSONObject parseJSON(String jsonDataPath) {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
		try {
			FileReader reader = new FileReader(jsonDataPath);
			jsonObject = (JSONObject) jsonParser.parse(reader);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return jsonObject;
	}
	
	private static void populateAndCopy(String originalPdf, String targetPdf) throws IOException {
//		printFields();  // Uncomment to see the fields in this document in console
		setFields();
		
		_pdfDocument.save(targetPdf);
		_pdfDocument.close();
	}

	private static void setFields() throws IOException {
		List<PDField> formFields;
		try {
			formFields = _acroForm.getFields();
		} catch (NullPointerException e) {
			formFields = new ArrayList<PDField>();
			System.out.println("Exception in _acroForm.getFields()");
		}
		
		Iterator<PDField> iter = formFields.iterator();
		while (iter.hasNext()) {
			PDField pdField = (PDField) iter.next();
			String key = pdField.getPartialName();
			if(_jsonObject.get(key) != null) {
				setField(key, _jsonObject.get(key).toString());
			}

			removeBorder(pdField);
		}
		
		try {
	//		some forms will not show the field content unless clicked in the field. But setting it readonly already made it unclickable
	//		so it will appear empty. Refreshing appearences fixes this
			if(_acroForm != null && _acroForm.getDefaultResources() != null) {
				_acroForm.refreshAppearances(filteredFields(formFields));
			}
		} catch (NullPointerException e) {
			System.out.println("Exception in _acroForm.refreshAppearances() \n " + e.getMessage());
		} catch (Exception e) {
			System.out.println("Exception in _acroForm.refreshAppearances() \n " + e.getMessage());
		}
	}

	private static void removeBorder(PDField pdField) {
		List<PDAnnotationWidget> widgets = pdField.getWidgets();
		Iterator<PDAnnotationWidget> widgetIter = widgets.iterator();
		while (widgetIter.hasNext()) {
			PDAnnotationWidget widget = widgetIter.next();
			widget.setBorderStyle(null);
		}
	}

	private static List<PDField> filteredFields(List<PDField> formFields) {
		Iterator<PDField> i = formFields.iterator();
		while (i.hasNext()) {
			PDField field = i.next();
			if (field.getClass().getSimpleName().equals("PDSignatureField")) {
			    i.remove();
			}
		}
		return formFields;
	}

	private static void setField(String name, String value ) throws IOException {
        PDField field = _acroForm.getField( name );
        if( field != null ) {
            field.setValue(value);
            field.setReadOnly(true);
        }
        else {
            System.err.println( "No field found with name:" + name );
        }
    }

	public static void printFields() throws IOException {
        List<PDField> fields = _acroForm.getFields();
        Iterator<PDField> fieldsIter = fields.iterator();

        System.out.println(new Integer(fields.size()).toString() + " top-level fields were found on the form");

        while( fieldsIter.hasNext()) {
            PDField field = (PDField)fieldsIter.next();
            processField(field, "|--", field.getPartialName());
        }
    }
    
	private static void processField(PDField field, String sLevel, String sParent) throws IOException {
         String outputString = sLevel + sParent + "." + field.getPartialName() + ",  type=" + field.getClass().getName();
         System.out.println(outputString);
    }
}
