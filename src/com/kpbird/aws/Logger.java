package com.kpbird.aws;



import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {
	private Logger instance;
	private  Class cls;
	private SimpleDateFormat sdf;
	private Logger(Class cls){
		this.cls = cls;
		sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	}
	public static Logger getInstance(Class cls){
		return new Logger(cls);
	}
	public void Info(String msg){
		System.out.println(getCurrientTime() + " | " + cls.getSimpleName() + " | " + msg);
	}
	private String getCurrientTime(){
		 
		 Date date = new Date();
		 return sdf.format(date);
	}
}
