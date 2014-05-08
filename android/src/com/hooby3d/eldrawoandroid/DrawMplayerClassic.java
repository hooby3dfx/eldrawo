package com.hooby3d.eldrawoandroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;

import android.graphics.PointF;
import android.util.Log;

import com.hooby3d.eldrawoandroid.MainActivity.Filter;
import com.hooby3d.eldrawoandroid.MainActivity.Mode;

public class DrawMplayerClassic {
	
	public interface MplayerClient{
		public void onMsgRcv(DrawMsg msg);
	}
	
	public class DrawMsg{
		
		int color;
		int size;
		boolean capt;
		Mode mode;
		Filter filter;
		float dragx;
		float dragy;
		float downx;
		float downy;
		
		int seq;
		
		public String toString(){
			StringBuilder b = new StringBuilder();
			b
				.append("☮")
				.append(color).append(',')
				.append(size).append(',')
				.append(capt).append(',')
				.append(mode.ordinal()).append(',')
				.append(" ").append(',')//.append(filter.ordinal()).append(',')
				.append(dragx).append(',')
				.append(dragy).append(',')
				.append(downx).append(',')
				.append(downy).append(',')
				.append(seq);
			return b.toString();
		}
	}
	
	private Socket socket;
	private PrintWriter out;
	private MplayerClient client;
	private boolean connected = false;
	private LinkedList<DrawMsg> messagesToSend = new LinkedList<DrawMplayerClassic.DrawMsg>();

	private static int msgCt = 0;
	
	public DrawMplayerClassic(MplayerClient c){
		client = c;
		msgCt = 0;
	}
	
	public boolean mplayerConnect(){

		try {
			Log.d("DrawMplayer", "mplayerConnect");
			socket = new Socket("digital.vacat.ion.com.ve", 18001);
			socket.setTcpNoDelay(true);
			Log.d("DrawMplayer", "setup socket");
			connected = true;
			new RcvThread().start();
			new TxThread().start();
			return true;
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	private class RcvThread extends Thread{
		public void run(){
			try{
				Log.d("DrawMplayer", "RcvThread started");
				
				BufferedReader in = new BufferedReader(
	                    new InputStreamReader(
	                    socket.getInputStream()));
				String inLine;
				Log.d("DrawMplayer", "RcvThread waiting");
				while((inLine = in.readLine()) !=null){
					//Log.d("DrawMplayer", "RcvThread got "+inLine);
					String[] vals = inLine.split(",");
					DrawMsg msg = new DrawMsg();
					msg.color = Integer.valueOf(vals[0]);
					msg.size = Integer.valueOf(vals[1]);
					msg.capt = Boolean.valueOf(vals[2]);
					msg.mode = Mode.values()[Integer.valueOf(vals[3])];
					//msg.filter = Filter.values()[Integer.valueOf(vals[4])];
					msg.dragx = Float.valueOf(vals[5]);
					msg.dragy = Float.valueOf(vals[6]);
					msg.downx = Float.valueOf(vals[7]);
					msg.downy = Float.valueOf(vals[8]);
					
					
					msg.seq = Integer.valueOf(vals[9]);
					
					client.onMsgRcv(msg);
					Log.d("DrawMplayer", "RcvThread waiting");
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			connected = false;
		}
	}
	
	private class TxThread extends Thread{
		public void run(){
			try{
				out = new PrintWriter(socket.getOutputStream(), true);
				while(connected){
					
					DrawMsg msg;
					while(!messagesToSend.isEmpty()){
						msg = messagesToSend.removeFirst();
						Log.d("DrawMplayer", "tx msg: "+msg.seq);
						out.println(msg.toString());
					}
					synchronized (messagesToSend) {
						messagesToSend.wait();
					}
					
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			//connected = false;
		}
	}

	public void send(DrawMsg msg) {
		msgCt++;
		msg.seq = msgCt;
		Log.d("DrawMplayer", "add msg: "+msgCt);
		
		messagesToSend.add(msg);
		synchronized (messagesToSend) {
			messagesToSend.notify();
		}
		
		
	}
}
