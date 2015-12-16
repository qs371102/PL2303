package com.prolific.pl2303hxdsimpletest;


import java.io.IOException;
import java.util.ArrayList;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tw.com.prolific.driver.pl2303.PL2303Driver;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;



public class PL2303HXDSimpleTest extends Activity implements INetworkCallback{

	private static String stringToSplite="\\s+";
	private ExecutorService pool = Executors.newFixedThreadPool(2); 
	//---------------------------------------------------
	private int formerCallbackCount;
	private List<Byte> finalCallBackData;

	private boolean needCallback;
	
	private long startTime;
	//20s
	private final long interval= (long) 20000;
	// debug settings
	// private static final boolean SHOW_DEBUG = false;
	private NetworkService mService;

	private static final boolean SHOW_DEBUG = true;

	// Defines of Display Settings
	private static final int DISP_CHAR = 0;

	// Linefeed Code Settings
	//    private static final int LINEFEED_CODE_CR = 0;
	private static final int LINEFEED_CODE_CRLF = 1;
	private static final int LINEFEED_CODE_LF = 2;

	PL2303Driver mSerial;

	//    private ScrollView mSvText;
	//   private StringBuilder mText = new StringBuilder();

	String TAG = "PL2303HXD_APLog";

	String DT="Debug";

	private Button btWrite;
	private EditText etWrite;

	private Button btRead;
	private EditText etRead;

	private Button btLoopBack;
	private ProgressBar pbLoopBack;    
	private TextView tvLoopBack;

	private Button btGetSN;  
	private TextView tvShowSN;

	private int mDisplayType = DISP_CHAR;
	private int mReadLinefeedCode = LINEFEED_CODE_LF;
	private int mWriteLinefeedCode = LINEFEED_CODE_LF;

	//BaudRate.B4800, DataBits.D8, StopBits.S1, Parity.NONE, FlowControl.RTSCTS
	private PL2303Driver.BaudRate mBaudrate = PL2303Driver.BaudRate.B9600;
	private PL2303Driver.DataBits mDataBits = PL2303Driver.DataBits.D8;
	private PL2303Driver.Parity mParity = PL2303Driver.Parity.NONE;
	private PL2303Driver.StopBits mStopBits = PL2303Driver.StopBits.S1;
	private PL2303Driver.FlowControl mFlowControl = PL2303Driver.FlowControl.OFF;


	private static final String ACTION_USB_PERMISSION = "com.prolific.pl2303hxdsimpletest.USB_PERMISSION";

	private static final String NULL = null;   

	// Linefeed
	//    private final static String BR = System.getProperty("line.separator");

	public Spinner PL2303HXD_BaudRate_spinner;
	public int PL2303HXD_BaudRate;
	public String PL2303HXD_BaudRate_str="B4800";

	private String strStr;





	@Override
	public void onCreate(Bundle savedInstanceState) {

		Log.d(DT, "=========Enter onCreate===========");

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pl2303_hxdsimple_test);

		mService=new NetworkService(this);
		mService.delegate=this;

		Log.d(TAG, "network Service!");


		//return;

		PL2303HXD_BaudRate_spinner = (Spinner)findViewById(R.id.spinner1);

		ArrayAdapter<CharSequence> adapter = 
				ArrayAdapter.createFromResource(this, R.array.BaudRate_Var, android.R.layout.simple_spinner_item);

		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		PL2303HXD_BaudRate_spinner.setAdapter(adapter);		
		PL2303HXD_BaudRate_spinner.setOnItemSelectedListener(new MyOnItemSelectedListener());

		Button mButton01 = (Button)findViewById(R.id.button1);
		mButton01.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {
				openUsbSerial();
			}
		});

		btWrite = (Button) findViewById(R.id.button2);        
		btWrite.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {
				etWrite = (EditText) findViewById(R.id.editText1);
				writeDataToSerial();
			}
		});

		btRead = (Button) findViewById(R.id.button3);        
		btRead.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {				
				etRead = (EditText) findViewById(R.id.editText2);
				readDataFromSerial();
			}
		});

		btLoopBack = (Button) findViewById(R.id.button4);        
		btLoopBack.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {				
				pbLoopBack = (ProgressBar) findViewById(R.id.ProgressBar1);
				setProgressBarVisibility(true);
				pbLoopBack.setIndeterminate(false);
				pbLoopBack.setVisibility(View.VISIBLE);
				pbLoopBack.setProgress(0);
				tvLoopBack = (TextView) findViewById(R.id.textView2);
				new Thread(tLoop).start();
			}
		});        


		btGetSN = (Button) findViewById(R.id.btn_GetSN);       
		tvShowSN = (TextView) findViewById(R.id.text_ShowSN);
		tvShowSN.setText("");
		btGetSN.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {				


				ShowPL2303HXD_SerialNmber();

			}
		});  
		// get service
		mSerial = new PL2303Driver((UsbManager) getSystemService(Context.USB_SERVICE),
				this, ACTION_USB_PERMISSION); 

		// check USB host function.
		if (!mSerial.PL2303USBFeatureSupported()) {

			Toast.makeText(this, "No Support USB host API", Toast.LENGTH_SHORT)
			.show();

			Log.d(TAG, "No Support USB host API");

			mSerial = null;

		}

		Log.d(TAG, "Leave onCreate");

		new android.os.Handler().postDelayed(
				new Runnable() {
					public void run() {
						Log.d(DT, "This'll run 500 milliseconds later");
						openUsbSerial();
					}
				}, 
				500);


	}//onCreate

	protected void onStop() {
		Log.d(TAG, "Enter onStop");
		super.onStop();        
		Log.d(TAG, "Leave onStop");
	}    

	@Override
	protected void onDestroy() {
		Log.d(TAG, "Enter onDestroy");   

		if(mSerial!=null) {
			mSerial.end();
			mSerial = null;
		}    	

		super.onDestroy();        
		Log.d(TAG, "Leave onDestroy");
	}    

	public void onStart() {
		Log.d(TAG, "Enter onStart");
		super.onStart();
		Log.d(TAG, "Leave onStart");
	}

	public void onResume() {
		Log.d(TAG, "Enter onResume"); 
		super.onResume();
		String action =  getIntent().getAction();
		Log.d(TAG, "onResume:"+action);

		//if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action))        
		if(!mSerial.isConnected()) {
			if (SHOW_DEBUG) {
				Log.d(TAG, "New instance : " + mSerial);
			}

			if( !mSerial.enumerate() ) {

				Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();     
				return;
			} else {
				Log.d(TAG, "onResume:enumerate succeeded!");
			}    		 
		}//if isConnected  
		Toast.makeText(this, "attached", Toast.LENGTH_SHORT).show();

		Log.d(TAG, "Leave onResume"); 
	}        

	/*
    public void SetNewVIDPID(){

    	 Log.d(TAG, "Enter SetNewVIDPID"); 
    	 String strVIDPID = etNewVIDPID.getText().toString();    	 
         Log.d(TAG, "new VID_PID : " + strVIDPID);

       	 if(!mSerial.isConnected()) {
             if (SHOW_DEBUG) {
              	  Log.d(TAG, "New instance : " + mSerial);
             }

    		 if( !mSerial.Set_NewVID_PID(strVIDPID) ) {
    			 Log.d(TAG, "SetNewVIDPID_2 : " + mSerial);
               	 return;
              } else {
                 Log.d(TAG, "onResume:enumerate succeeded!");
              }    		 
          }//if isConnected  
		  Toast.makeText(this, "attached", Toast.LENGTH_SHORT).show();
		  Log.d(TAG, "attached!");		
		  openUsbSerial();       	
          Log.d(TAG, "Leave SetNewVIDPID");     	
    }
	 */
	private void openUsbSerial() {
		Log.d(TAG, "Enter  openUsbSerial");


		if(null==mSerial)
			return;   	 

		if (mSerial.isConnected()) {
			if (SHOW_DEBUG) {
				Log.d(TAG, "openUsbSerial : isConnected ");
			}
			String str = PL2303HXD_BaudRate_spinner.getSelectedItem().toString();
			int baudRate= Integer.parseInt(str);
			switch (baudRate) {
			case 9600:
				mBaudrate = PL2303Driver.BaudRate.B9600;
				break;
			case 19200:
				mBaudrate =PL2303Driver.BaudRate.B19200;
				break;
			case 115200:
				mBaudrate =PL2303Driver.BaudRate.B115200;
				break;
			default:
				mBaudrate =PL2303Driver.BaudRate.B9600;
				break;
			}   		            
			Log.d(TAG, "baudRate:"+baudRate);
			// if (!mSerial.InitByBaudRate(mBaudrate)) {
			if (!mSerial.InitByBaudRate(mBaudrate,700)) {
				if(!mSerial.PL2303Device_IsHasPermission()) {
					Toast.makeText(this, "cannot open, maybe no permission", Toast.LENGTH_SHORT).show();		
				}

				if(mSerial.PL2303Device_IsHasPermission() && (!mSerial.PL2303Device_IsSupportChip())) {
					Toast.makeText(this, "cannot open, maybe this chip has no support, please use PL2303HXD / RA / EA chip.", Toast.LENGTH_SHORT).show();
				}
			} else {        	

				Toast.makeText(this, "connected : " , Toast.LENGTH_SHORT).show(); 	

			}
		}//isConnected

		Log.d(TAG, "Leave openUsbSerial");


		//----------------------------------

	}//openUsbSerial


	private void readDataFromSerial() {

		int len;
		// byte[] rbuf = new byte[4096];
		byte[] rbuf = new byte[20];
		StringBuffer sbHex=new StringBuffer();

		Log.d(TAG, "Enter readDataFromSerial");

		if(null==mSerial)
			return;        

		Log.d(TAG, "Not null");
		if(!mSerial.isConnected()) 
			return;

		Log.d(TAG, "Connected");

		len = mSerial.read(rbuf);

		if(len<0) {
			Log.d(TAG, "Fail to bulkTransfer(read data)");
			return;
		}

		if (len > 0) {        	
			if (SHOW_DEBUG) {
				Log.d(TAG, "read len : " + len);
			}                
			//rbuf[len] = 0;
			for (int j = 0; j < len; j++) {            	   
				String temp=Integer.toHexString(rbuf[j]&0x000000FF);
				Log.w(TAG, "str_rbuf["+j+"]="+temp);
				int decimal = Integer.parseInt(temp);
				sbHex.append((char) (decimal));
			}              
			etRead.setText(sbHex.toString()); 
			Log.w(TAG, "======="+sbHex.toString()+"========");
			//mService.sendCommand(sbHex.toString());
			//Toast.makeText(this, "len="+len, Toast.LENGTH_SHORT).show();
		}
		else {     	
			if (SHOW_DEBUG) {
				Log.d(TAG, "read=============serial len : 0 ");
			}
			etRead.setText("empty");
			return;
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.d(TAG, "Leave readDataFromSerial");	
	}//readDataFromSerial

	private void writeDataToSerial() {

		Log.d(TAG, "Enter writeDataToSerial");

		if(null==mSerial)
			return;

		if(!mSerial.isConnected()) 
			return;

		String strWrite = etWrite.getText().toString();

		/*
        //strWrite="012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
       // strWrite = changeLinefeedcode(strWrite);
         strWrite="012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789";
         if (SHOW_DEBUG) {
            Log.d(TAG, "PL2303Driver Write(" + strWrite.length() + ") : " + strWrite);
        }
        int res = mSerial.write(strWrite.getBytes(), strWrite.length());
		if( res<0 ) {
			Log.d(TAG, "setup: fail to controlTransfer: "+ res);
			return;
		} 

		Toast.makeText(this, "Write length: "+strWrite.length()+" bytes", Toast.LENGTH_SHORT).show();  
		 */
		// test data: 600 byte
		//strWrite="AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
		if (SHOW_DEBUG) {
			Log.d(TAG, "PL2303Driver Write 2(" + strWrite.length() + ") : " + strWrite);
		}
		//int res = mSerial.write(strWrite.getBytes(), strWrite.length());

		byte[] datas=new byte[3];
		datas[0]=(byte)138;
		datas[1]=(byte)0;
		datas[2]=(byte)138;

		int res = mSerial.write(datas, datas.length);
		//readDataFromSerial();
		if( res<0 ) {
			Log.d(TAG, "setup2: fail to controlTransfer: "+ res);
			//readDataFromSerial();
			return;
		} 

		Toast.makeText(this, "Write length: "+strWrite.length()+" bytes", Toast.LENGTH_SHORT).show(); 

	}//writeDataToSerial


	private void ShowPL2303HXD_SerialNmber() {



		Log.d(TAG, "Enter ShowPL2303HXD_SerialNmber");

		if(null==mSerial)
			return;        

		if(!mSerial.isConnected()) 
			return;

		if(mSerial.PL2303Device_GetSerialNumber()!=NULL) {
			tvShowSN.setText(mSerial.PL2303Device_GetSerialNumber());

		}
		else{
			tvShowSN.setText("No SN");

		}

		Log.d(TAG, "Leave ShowPL2303HXD_SerialNmber");	
	}//ShowPL2303HXD_SerialNmber

	//------------------------------------------------------------------------------------------------//
	//--------------------------------------LoopBack function-----------------------------------------//    
	//------------------------------------------------------------------------------------------------//    
	private static final int START_NOTIFIER = 0x100;
	private static final int STOP_NOTIFIER = 0x101;
	private static final int PROG_NOTIFIER_SMALL = 0x102;
	private static final int PROG_NOTIFIER_LARGE = 0x103;
	private static final int ERROR_BAUDRATE_SETUP = 0x8000;
	private static final int ERROR_WRITE_DATA = 0x8001;
	private static final int ERROR_WRITE_LEN = 0x8002;
	private static final int ERROR_READ_DATA = 0x8003;
	private static final int ERROR_READ_LEN = 0x8004;
	private static final int ERROR_COMPARE_DATA = 0x8005;

	Handler myMessageHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what){
			case START_NOTIFIER:
				pbLoopBack.setProgress(0);
				tvLoopBack.setText("LoopBack Test start...");
				btWrite.setEnabled(false);
				btRead.setEnabled(false);
				break;
			case STOP_NOTIFIER:
				pbLoopBack.setProgress(pbLoopBack.getMax());
				tvLoopBack.setText("LoopBack Test successfully!");
				btWrite.setEnabled(true);
				btRead.setEnabled(true);
				break;
			case PROG_NOTIFIER_SMALL:
				pbLoopBack.incrementProgressBy(5);
				break;
			case PROG_NOTIFIER_LARGE:
				pbLoopBack.incrementProgressBy(10);					
				break;	
			case ERROR_BAUDRATE_SETUP:					
				tvLoopBack.setText("Fail to setup:baudrate "+msg.arg1);
				break;
			case ERROR_WRITE_DATA:
				tvLoopBack.setText("Fail to write:"+ msg.arg1);				
				break;
			case ERROR_WRITE_LEN:
				tvLoopBack.setText("Fail to write len:"+msg.arg2+";"+ msg.arg1);
				break;
			case ERROR_READ_DATA:
				tvLoopBack.setText("Fail to read:"+ msg.arg1);					
				break;
			case ERROR_READ_LEN:
				tvLoopBack.setText("Length("+msg.arg2+") is wrong! "+ msg.arg1);
				break;
			case ERROR_COMPARE_DATA:
				tvLoopBack.setText("wrong:"+ 
						String.format("rbuf=%02X,byteArray1=%02X", msg.arg1, msg.arg2));
				break;

			}	
			super.handleMessage(msg);
		}//handleMessage
	};

	private void Send_Notifier_Message(int mmsg) {
		Message m= new Message();
		m.what = mmsg;
		myMessageHandler.sendMessage(m);
		Log.d(TAG, String.format("Msg index: %04x", mmsg));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void Send_ERROR_Message(int mmsg, int value1, int value2) {
		Message m= new Message();
		m.what = mmsg;
		m.arg1 = value1;
		m.arg2 = value2;
		myMessageHandler.sendMessage(m);
		Log.d(TAG, String.format("Msg index: %04x", mmsg));
	}	

	private Runnable tLoop = new Runnable() {
		public void run() {	

			int res = 0, len, i;
			Time t = new Time();
			byte[] rbuf = new byte[4096];    	
			final int mBRateValue[] = {9600, 19200, 115200};    	
			PL2303Driver.BaudRate mBRate[] = {PL2303Driver.BaudRate.B9600, PL2303Driver.BaudRate.B19200, PL2303Driver.BaudRate.B115200};

			if(null==mSerial)
				return;            	    	

			if(!mSerial.isConnected()) 
				return;		

			t.setToNow();
			//Random mRandom = new Random(t.toMillis(false));

			//byte[] byteArray1 = new byte[256]; //test pattern-1    	
			//mRandom.nextBytes(byteArray1);//fill buf with random bytes

			byte[] byteArray1 = new byte[3]; //test pattern-1    	
			byteArray1[0]=(byte)138;
			byteArray1[1]=(byte)0;
			byteArray1[2]=(byte)138;

			Send_Notifier_Message(START_NOTIFIER);    	

			for(int WhichBR=0;WhichBR<mBRate.length;WhichBR++) {

				try {
					res = mSerial.setup(mBRate[WhichBR], mDataBits, mStopBits, mParity, mFlowControl);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if( res<0 ) {
					Send_Notifier_Message(START_NOTIFIER);
					Send_ERROR_Message(ERROR_BAUDRATE_SETUP, mBRateValue[WhichBR], 0);					
					Log.d(TAG, "Fail to setup="+res);
					return;
				} 
				Send_Notifier_Message(PROG_NOTIFIER_LARGE);

				for(int times=0;times<1;times++) {

					len = mSerial.write(byteArray1, byteArray1.length);
					if( len<0 ) {
						Send_ERROR_Message(ERROR_WRITE_DATA, mBRateValue[WhichBR], 0);		       			
						Log.d(TAG, "Fail to write="+len);
						return;
					}

					if( len!=byteArray1.length ) {		       			
						Send_ERROR_Message(ERROR_WRITE_LEN, mBRateValue[WhichBR], len);		       			
						return;
					} 
					Send_Notifier_Message(PROG_NOTIFIER_SMALL);

					len = mSerial.read(rbuf);
					if(len<0) {
						Send_ERROR_Message(ERROR_READ_DATA, mBRateValue[WhichBR], 0);
						return;
					}
					Log.d(TAG, "read length="+len+";byteArray1 length="+byteArray1.length);

					if(len!=byteArray1.length) {		    	    	
						Send_ERROR_Message(ERROR_READ_LEN, mBRateValue[WhichBR], len);
						return;    	    	
					}  		
					Send_Notifier_Message(PROG_NOTIFIER_SMALL);

					for(i=0;i<len;i++) {
						if(rbuf[i]!=byteArray1[i]) {	
							Send_ERROR_Message(ERROR_COMPARE_DATA, rbuf[i], byteArray1[i]);		    	    		
							Log.d(TAG, "Data is wrong at "+ 
									String.format("rbuf[%d]=%02X,byteArray1[%d]=%02X", i, rbuf[i], i, byteArray1[i]));
							return;    	    	    	    		
						}//if
					}//for
					Send_Notifier_Message(PROG_NOTIFIER_LARGE);

				}//for(times)    	    

			}//for(WhichBR)

			try {
				res = mSerial.setup(mBaudrate, mDataBits, mStopBits, mParity, mFlowControl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if( res<0 ) {
				Send_ERROR_Message(ERROR_BAUDRATE_SETUP, 0, 0);				
				return;
			}   		    		    			
			Send_Notifier_Message(STOP_NOTIFIER);    	

		}//run()
	};//Runnable tLoop






	public class MyOnItemSelectedListener implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

			if(null==mSerial)
				return;

			if(!mSerial.isConnected()) 
				return;

			int baudRate=0;
			String newBaudRate;
			Toast.makeText(parent.getContext(), "newBaudRate is-" + parent.getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
			newBaudRate= parent.getItemAtPosition(position).toString();

			try	{
				baudRate= Integer.parseInt(newBaudRate);
			}
			catch (NumberFormatException e)	{
				System.out.println(" parse int error!!  " + e);
			}

			switch (baudRate) {
			case 9600:
				mBaudrate =PL2303Driver.BaudRate.B9600;
				break;
			case 19200:
				mBaudrate =PL2303Driver.BaudRate.B19200;
				break;
			case 115200:
				mBaudrate =PL2303Driver.BaudRate.B115200;
				break;
			default:
				mBaudrate =PL2303Driver.BaudRate.B9600;
				break;
			}   			 

			int res = 0;
			try {
				res = mSerial.setup(mBaudrate, mDataBits, mStopBits, mParity, mFlowControl);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if( res<0 ) {
				Log.d(TAG, "fail to setup");
				return;
			}
		}
		public void onNothingSelected(AdapterView<?> parent) {
			// Do nothing.    
		}
	}//MyOnItemSelectedListener

	@Override
	public void receiveData(String data) {
		// TODO Auto-generated method stub
		Log.d(DT, "receive command:"+data);
		String strWrite = data;

		byte[] datas=parseStringToData(data);
		if(datas==null)
			return;

		if(null==mSerial)
			return;

		if(!mSerial.isConnected()) 
			return;
		
		
		int res = mSerial.write(datas, datas.length);
		Log.d(DT, "mSerial.write  res:"+res);
//		try{
//			Thread.sleep(5000);
//		}catch(Exception ex)
//		{
//			Log.w(DT, "exception:"+ex.toString());
//		}
		if( res<0 ) {
			Log.d(DT, "setup2: fail to controlTransfer: "+ res);
			return;
		}else
		{
			Toast.makeText(this, "Write length: "+datas.length+" bytes", Toast.LENGTH_SHORT).show();
			if(!needCallback)
				return;
			
			pool.execute(tReadCallback);
		}
	}


	
//	Handler networkMessageHandler = new Handler() {
//		public void handleMessage(Message msg) {
//			mService.sendCommand(msg.obj.toString());
//			super.handleMessage(msg);
//		}//handleMessage
//	};
//	
//	private void Send_Network_Message(String mmsg) {
//		Message m= new Message();
//		m.obj = mmsg;
//		networkMessageHandler.sendMessage(m);
//		Log.d(TAG, String.format("Msg index: %04x", mmsg));
//		try {
//			Thread.sleep(1000);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}
	
	private Runnable tReadCallback = new Runnable() {
		public void run() {	
			finalCallBackData=new ArrayList<Byte>();

			startTime=System.currentTimeMillis();
			
			while(false||(System.currentTimeMillis()-startTime)<interval)
			{
				if(!ifContinueWaitToReadCallBackFromSerial())
				{
					break;
				}
				try{
					Thread.sleep(1000);
				}catch(Exception ex)
				{
					Log.w(DT, "exception:"+ex.toString());
				}
			}
		}
	};
	
	//TODO tmp

	public byte[] parseStringToData(String data)
	{
		final String[] datas = data.split(":");

		if(datas.length==4)
		{

			mService.SetTargetID(datas[0]);

			if(!mService.isMine(datas[1]))
			{
				return null;
			}
			byte[] finalBytes=hexStr2Bytes(datas[2]);
			needCallback= Boolean.valueOf(datas[3]);
			return finalBytes;
		}
		else
		{
			Log.d(TAG, "not mine");
			return null;
		}
	}


	public static byte[] hexStr2Bytes(String src){  

		String[] datas = src.trim().split(stringToSplite);  

		byte[] finalData=new byte[datas.length];
		Log.d("PL2303HXD_APLog", "ex:"+datas.length);
		for (int i=0;i<datas.length;i++) {

			int tmp=Integer.parseInt(datas[i],16);
			//Log.d("Debug", i+"==="+tmp);
			finalData[i]=(byte)tmp;
		}
		return finalData;  
	}  

	public boolean ifContinueWaitToReadCallBackFromSerial()
	{
		int len;
		byte[] rbuf = new byte[1024];

		if(null==mSerial)
			return false;        

		if(!mSerial.isConnected()) 
			return false;

		Log.d(TAG, "Connected");

		len = mSerial.read(rbuf);

		if(len<0) {
			Log.d(DT, "Fail to bulkTransfer(read data)");
			return true;
		}
		//符合条件读取结束
		if(formerCallbackCount>0&&len==0)
		{
			//置0
			formerCallbackCount=len;
			
			//Log.w(DT, "finalCallbackData:"+formerCallbackCount);

			String message="";
			for (int i=0;i<finalCallBackData.size();i++) 
			{   
				Byte b=finalCallBackData.get(i);
				String temp=Integer.toHexString(b&0x000000FF);
				if(i+1<finalCallBackData.size())
					message+=temp+" ";
				else
					message+=temp;
				//Log.d(DT, " "+temp+" ");
			}
			//Log.w(DT,"+++++"+message+"+++++");
			if(message.length()!=0)
			{
				mService.sendCommand(message);
			}
			else
				return true;
			//Log.d(DT,"fcbd"+ finalCallBackData.toString());
			return false;
		}

		formerCallbackCount=len;
		

		if (len > 0) {        	
			if (SHOW_DEBUG) {
				Log.d(DT, "read len >0 : " + len);
			}                

			Log.d(DT, "=================start=================");
			for (int j = 0; j < len; j++) {            	   
				String temp=Integer.toHexString(rbuf[j]&0x000000FF);
				//Log.w(DT, "str_rbuf["+j+"]="+temp);
				finalCallBackData.add(rbuf[j]);
			}
			Log.d(DT, "=================end===================");
		}
		else {     	
			if (SHOW_DEBUG&&false) {
				Log.d(DT, "read len : 0 ");
			}
			return true;
		}

		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		Log.d(DT, "Leave readDataFromSerial");	
		return true;
	}

}
