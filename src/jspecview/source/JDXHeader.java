package jspecview.source;

import javajs.util.Lst;

/**
 * could be a spectrum or a source
 * 
 * @author Bob Hanson
 * 
 */
public class JDXHeader {

  public String title = "";
  public String jcampdx = "5.01";
  public String dataType = "";
  public String dataClass = "";
  public String origin = "";
  public String owner = "PUBLIC DOMAIN";
  public String longDate = "";
  public String date = "";
  public String time = "";


  /**
   * Sets the title of the spectrum
   * 
   * @param title
   *        the spectrum title
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * Sets the JCAMP-DX version number
   * 
   * @param versionNum
   *        the JCAMP-DX version number
   */
  public void setJcampdx(String versionNum) {
    this.jcampdx = versionNum;
  }

  /**
   * Sets the data type
   * 
   * @param dataType
   *        the data type
   */
  public void setDataType(String dataType) {
    this.dataType = dataType;
  }

  /**
   * Sets the data class
   * 
   * @param dataClass
   *        the data class
   */
  public void setDataClass(String dataClass) {
    this.dataClass = dataClass;
  }

  /**
   * Sets the origin of the JCAMP-DX spectrum
   * 
   * @param origin
   *        the origin
   */
  public void setOrigin(String origin) {
    this.origin = origin;
  }

  /**
   * Sets the owner
   * 
   * @param owner
   *        the owner
   */
  public void setOwner(String owner) {
    this.owner = owner;
  }

  /**
   * Sets the long date of when the file was created
   * 
   * @param longDate
   *        String
   */
  public void setLongDate(String longDate) {
    this.longDate = longDate;
  }

  /**
   * Sets the date the file was created
   * 
   * @param date
   *        String
   */
  public void setDate(String date) {
    this.date = date;
  }

  /**
   * Sets the time the file was created
   * 
   * @param time
   *        String
   */
  public void setTime(String time) {
    this.time = time;
  }


  /**
   * Getter for property title.
   * 
   * @return Value of property title.
   */
  public String getTitle() {
    return title;
  }

  final static String[] typeNames = {
  	"ND NMR SPECTRUM   NMR",
  	"NMR SPECTRUM      NMR",
  	"INFRARED SPECTRUM IR",
  	"MASS SPECTRUM     MS",
  	"RAMAN SPECTRUM    RAMAN",
  	"GAS CHROMATOGRAM  GC",
  	"UV/VIS SPECTRUM   UV/VIS"
  };
  
  private String qualifiedType;
  
  static String getTypeName(String type) {
  	type = type.toUpperCase();
  	for (int i = 0; i < typeNames.length; i++)
  		if (typeNames[i].startsWith(type)) {
  			return typeNames[i].substring(18);
  		}
  	return type;
  }

  public String getQualifiedDataType() {
  	return (qualifiedType == null ? (qualifiedType = getTypeName(dataType)) : qualifiedType);
  }
  /**
   * Getter for property jcampdx.
   * 
   * @return Value of property jcampdx.
   */
  public String getJcampdx() {
    return jcampdx;
  }

  /**
   * Getter for property dataType.
   * 
   * @return Value of property dataType.
   */
  public String getDataType() {
    return dataType;
  }

  /**
   * Getter for property origin.
   * 
   * @return Value of property origin.
   */
  public String getOrigin() {
    return origin;
  }

  /**
   * Getter for property owner.
   * 
   * @return Value of property owner.
   */
  public String getOwner() {
    return owner;
  }

  /**
   * Getter for property longDate.
   * 
   * @return Value of property longDate.
   */
  public String getLongDate() {
    return longDate;
  }

  /**
   * Getter for property date.
   * 
   * @return Value of property date.
   */
  public String getDate() {
    return date;
  }

  /**
   * Getter for property time.
   * 
   * @return Value of property time.
   */
  public String getTime() {
    return time;
  }

  /**
   * Returns the data class
   * spectrum only
   * @return the data class
   */
  public String getDataClass() {
    return dataClass;
  }

  // Table of header variables specific to the jdx source or spectrum
  protected Lst<String[]> headerTable = new Lst<String[]>();
  
  /**
   * Sets the headerTable for this Source or spectrum
   * 
   * @param table
   *        the header table
   */
  public void setHeaderTable(Lst<String[]> table) {
    headerTable = table;
  }

  /**
   * Returns the table of headers
   * 
   * @return the table of headers
   */
  public Lst<String[]> getHeaderTable() {
    return headerTable;
  }

  public String[][] getHeaderRowDataAsArray(boolean addDataClass, int nMore) {
    
    String[][] rowData = new String[(addDataClass ? 6 : 5) + headerTable.size() + nMore][];

    int i = 0;
    rowData[i++] = new String[] { "##TITLE", title };
    rowData[i++] = new String[] { "##JCAMP-DX", jcampdx };
    rowData[i++] = new String[] { "##DATA TYPE", dataType };
    if (addDataClass)
      rowData[i++] = new String[] { "##DATA CLASS", dataClass };      
    rowData[i++] = new String[] { "##ORIGIN", origin };
    rowData[i++] = new String[] { "##OWNER", owner };
    
    for(int j = 0; j < headerTable.size(); j++)
      rowData[i++] = getRow(j);
    
    return rowData;
  }

	private String[] getRow(int j) {
		String[] s = headerTable.get(j);
		/**
		 * @j2sNative
		 * 
		 * return [s[0], javajs.util.PT.rep(s[1], "<", "&lt;")];
		 * 
		 */
		{
			return s;
		}
	}

  
  
}
