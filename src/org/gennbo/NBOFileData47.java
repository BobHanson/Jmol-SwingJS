package org.gennbo;


class NBOFileData47 {
  String preParams = "";
  String noFileKeywords = "";
  String postKeywordData = "";
  String allKeywords = "";
  
  NBOFileData47() { }
  
  NBOFileData47 set(String preParams, String allKeywords, String postKeywordData) {
    this.preParams = preParams;
    this.noFileKeywords = NBOUtil.removeNBOFileKeyword(allKeywords, null);
    this.allKeywords = allKeywords;
    this.postKeywordData = postKeywordData;
    return this;
  }
  
}
