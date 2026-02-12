package fr.orsay.lri.varna.applications.fragseq;

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.util.Hashtable;
import java.util.Random;

import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation;
import fr.orsay.lri.varna.models.annotations.ChemProbAnnotation.ChemProbAnnotationType;
import fr.orsay.lri.varna.models.rna.ModeleBase;
import fr.orsay.lri.varna.models.rna.RNA;

public class FragSeqAnnotationDataModel extends FragSeqModel {
  private String _id;
  private String _name;
  private Hashtable<Integer, ChemProbModel> _values = new Hashtable<Integer, ChemProbModel>();  
  
  
  public FragSeqAnnotationDataModel(String id, String name)
  {
	_id = id;
	_name = name;
  }

  public FragSeqAnnotationDataModel()
  {
	this(Long.toHexString(Double.doubleToLongBits(Math.random())),Long.toHexString(Double.doubleToLongBits(Math.random())));
  }

  public void addValue(ChemProbModel cpm)
  {
	  _values.put(cpm._baseNumber1,cpm);
  }

  static Random _rnd = new Random();
  
  public static void addRandomAnnotations(RNA r,FragSeqAnnotationDataModel data){
	  int nb = r.getSize()/5+_rnd.nextInt(r.getSize()/3);
	  Color[] colors = {Color.orange,Color.black,Color.blue.darker(),Color.green.darker(), Color.gray};
	  ChemProbAnnotationType[] types  = ChemProbAnnotationType.values();
	  for(int i=0;i<nb;i++)
	  {
		  int index = _rnd.nextInt(r.getSize()-1);
		  int number1 = r.getBaseNumber(index);
		  int number2 = r.getBaseNumber(index+1);
		  ChemProbModel cpm = data.new ChemProbModel(number1,number2,colors[_rnd.nextInt(colors.length)],2*_rnd.nextDouble(),types[_rnd.nextInt(types.length)],true);
		  data.addValue(cpm);
	  }
  }

  
  public String toString()
  {
	  return _name;
  }
  
  public String getID()
  {
	  return _id;
  }

  public void applyTo(RNA r)
  {
	r.clearChemProbAnnotations();
	for (ChemProbModel c : _values.values())
	{
		c.applyTo(r);
	}
  }

  
  public class ChemProbModel
  {
		private Color _color;
		private double _intensity;
		private ChemProbAnnotationType _type;
		private boolean _out;
		private int _baseNumber1;
		private int _baseNumber2;
		
		public ChemProbModel (int baseNumber1,int baseNumber2, Color color, double intensity, ChemProbAnnotationType type, boolean out)
		{
			_color= color;
			_intensity = intensity;
			_type= type;
			_out= out;
			_baseNumber1 = baseNumber1;
			_baseNumber2 = baseNumber2;
		}

		public void applyTo(RNA r)
		{
			System.out.println(this);
			int i = r.getIndexFromBaseNumber(_baseNumber1);
			int j = r.getIndexFromBaseNumber(_baseNumber2);
			if (i!=-1 && j!=-1)
			{
				ModeleBase mb1 = r.getBaseAt(i);
				ModeleBase mb2 = r.getBaseAt(j);
				r.addChemProbAnnotation(new ChemProbAnnotation(mb1, mb2, _type, _intensity,_color, _out));
			}
		}
		
		public String toString()
		{
			return ""+_baseNumber1+": col="+_color+" int="+_intensity+" type="+_type+" out="+_out;
		}
  }
  
  public static DataFlavor Flavor = new DataFlavor(FragSeqAnnotationDataModel.class, "RNA Chem Prob Data");

}
