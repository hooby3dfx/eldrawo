package com.hooby3d.eldrawoandroid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.Random;

import jerklib.Channel;
import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.ChannelListEvent;
import jerklib.events.IRCEvent;
import jerklib.events.IRCEvent.Type;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.listeners.IRCEventListener;

import android.util.Log;
import android.widget.ArrayAdapter;

import com.hooby3d.eldrawoandroid.MainActivity.Filter;
import com.hooby3d.eldrawoandroid.MainActivity.Mode;

public class DrawMplayerIRC implements IRCEventListener{
	public interface MplayerClient{
		public void onMsgRcv(DrawMsg msg);
	}
	public interface RoomListener{
		public void add(String room);
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
				//.append("☮")
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
	

	private MplayerClient client;
	private Channel room;
	private Session session;
	private String nick;
	private RoomListener roomListener;
	private boolean connected = false;

	private static int msgCt = 0;
	
	public DrawMplayerIRC(MplayerClient c, String nick){
		client = c;
		msgCt = 0;
		this.nick = generateRandomString();//nick;
		Log.d("DMIRC", this.nick);
	}
	
	public boolean mplayerConnect(){
		ConnectionManager conman = new ConnectionManager(new Profile(nick));
		conman.requestConnection("digital.vacat.ion.com.ve", 18002).addIRCEventListener(this);
		return true;
	}
	
	public void send(DrawMsg msg) {
		msgCt++;
		msg.seq = msgCt;
		Log.d("DrawMplayer", "add msg: "+msgCt);

		if(room!=null){
			room.say(msg.toString());
		}
		
		
	}

	@Override
	public void receiveEvent(IRCEvent e) {
		if(e.getType() == Type.CONNECT_COMPLETE)
		{
			connected = true;
			e.getSession().join("#lobby");
			e.getSession().chanList();
			session = e.getSession();
		}
		else if(e.getType() == Type.JOIN_COMPLETE)
		{
			JoinCompleteEvent jce = (JoinCompleteEvent)e;
			//jce.getChannel().say("Hello World!!");
			room = jce.getChannel();
		}
		else if(e.getType()==Type.CHANNEL_MESSAGE){
			MessageEvent msge = (MessageEvent)e;
			
			String[] vals = msge.getMessage().split(",");
			if(vals.length!=10)
				return;
			try{
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
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		else if(e.getType() == Type.CHANNEL_LIST_EVENT)
		{
			ChannelListEvent cle = (ChannelListEvent)e;
			if(cle.getChannelName().charAt(0)!='#')
				return;
			String roomInfo = cle.getChannelName().substring(1) + " (" + cle.getNumberOfUser()+")";
			roomListener.add(roomInfo);
		}
		else
		{
			System.out.println(e.getType() + " : " + e.getRawEventData());
		}
		
	}
	
	public void joinRoom(String name){
		if(name==null||name.length()==0){
			name = "shameroom";
		}
		if(room!=null){
			room.part(room.getName());
		}
		if(session!=null){
			name = name.trim();
			if(name.charAt(0)!='#'){
				name = "#"+name;
			}
			while(name.indexOf(' ')!=-1){
				name = name.substring(0, name.indexOf(' '));
			}
			if(name.length()<=1){
				name = "#shameroom";
			}
			
			session.join(name);
		}
	}
	
	public boolean isConnected(){
		return connected;
	}
	
	public void getList(){
		if(session!=null){
			session.chanList();
		}
	}

	public void setRoomAdapter(RoomListener roomAdapt) {
		this.roomListener = roomAdapt;
		
	}
	
	private static String generateRandomString(){
		String out = "";
		Random rand = new Random();
		char c;
		for(int i = 0; i < 9; i++){
			c = (char)(65 + rand.nextInt(26));
			out += c;
		}
		return out;
	}

}
