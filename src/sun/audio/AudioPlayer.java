package sun.audio;

import javajs.util.JSThread;

import java.io.InputStream;

import javax.sound.sampled.UnsupportedAudioFileException;

import swingjs.api.JSUtilI;

public class AudioPlayer extends JSThread {

static boolean isJS = /** @j2sNative true || */false;

static JSUtilI jsutil;


static {
  try {
    if (isJS) {
      jsutil = ((JSUtilI) Class.forName("swingjs.JSUtil").newInstance());
    }

  } catch (Exception e) {
    System.err.println("AudioPlayer could not create swinjs.JSUtil instance");
  }
}



	// note that this thread is never really started because the 
	// playAudio method is run anyway
  public static final AudioPlayer player = getAudioPlayer();

  private static AudioPlayer getAudioPlayer() {
    return new AudioPlayer();
}

	@Override
	protected boolean myInit() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isLooping() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean myLoop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void whenDone() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected int getDelayMillis() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	protected void onException(Exception e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void doFinally() {
		// TODO Auto-generated method stub
		
	}

	public void start(InputStream is) {
		// TODO -- support standard stream?
		AudioDataStream ads = (AudioDataStream) is;
		try {
			jsutil.playAudio(ads.getAudioData().buffer, ads.getAudioData().format);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


}
