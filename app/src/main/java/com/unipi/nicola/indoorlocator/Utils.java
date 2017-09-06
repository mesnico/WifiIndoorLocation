package com.unipi.nicola.indoorlocator;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

/**
 * Created by Nicola on 24/08/2017.
 */

public class Utils {
    /**
     * sends a message using the supplied messenger
     * @param to the destination
     * @param msgType the message
     * @param b bundle to send with the message
     */
    public static void sendMessage(Messenger to, int msgType, Bundle b, Messenger replyTo){
        Message msg = Message.obtain(null, msgType);
        if (b != null){
            msg.setData(b);
        }
        try {
            if(to != null) {
                msg.replyTo = replyTo;
                to.send(msg);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
