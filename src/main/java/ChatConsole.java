import gearth.extensions.Extension;
import gearth.extensions.ExtensionInfo;
import gearth.extensions.IExtension;
import gearth.extensions.extra.hashing.HashSupport;
import gearth.protocol.HMessage;
import gearth.protocol.HPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Jonas on 3/12/2018.
 */

/**
 * Must be created in initextension
 */
public class ChatConsole {

    private volatile int chatid;
    private volatile String name;
    private volatile HashSupport hashSupport;
    private volatile IExtension extension;

    private volatile boolean firstTime = true;


    public ChatConsole(final HashSupport hashSupport, IExtension extension) {
        this.extension = extension;
        this.hashSupport = hashSupport;
        this.name = extension.getClass().getAnnotation(ExtensionInfo.class).Title();
        chatid = this.name.hashCode();

        final boolean[] doOncePerConnection = {false};

        extension.onConnect(new Extension.OnConnectionListener() {
            public void act(String s, int i, String s1) {
                doOncePerConnection[0] = true;
            }
        });

        extension.intercept(HMessage.Side.TOSERVER, new Extension.MessageListener() {
            public void act(HMessage hMessage) {
                // if the first packet on init is not 4000, the extension was already running, so we open the chat instantly
                if (firstTime) {
                    firstTime = false;
                    if (hMessage.getPacket().headerId() != 4000) {
                        doOncePerConnection[0] = false;
                        createChat();
                    }
                }
            }
        });

        hashSupport.intercept(HMessage.Side.TOCLIENT, "Friends", new Extension.MessageListener() {
            public void act(HMessage hMessage) {
                if (doOncePerConnection[0]) {
                    doOncePerConnection[0] = false;

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                createChat();

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                }
            }
        });

        hashSupport.intercept(HMessage.Side.TOSERVER, "FriendPrivateMessage", new Extension.MessageListener() {
            public void act(HMessage hMessage) {
                HPacket packet = hMessage.getPacket();
                if (packet.readInteger() == chatid) {
                    hMessage.setBlocked(true);
                    notifyChatInputListeners(packet.readString());
                }
            }
        });
    }

    private void createChat() {
        hashSupport.sendToClient("UpdateFriend",
                0, 1, false, false, "", chatid, " [G-Earth] - " + name, 1, true, false, "", 0, "", 0, true, true, true, ""
        );
    }

    public void writeOutput(boolean asInvite) {
        //TODO
    }

    public interface ChatInputListener {
        void inputEntered(String input);
    }
    private List<ChatInputListener> chatInputListenerList = new ArrayList<ChatInputListener>();
    public void onInput(ChatInputListener listener) {
        chatInputListenerList.add(listener);
    }
    private void notifyChatInputListeners (String s) {
        for (ChatInputListener listener : chatInputListenerList) {
            listener.inputEntered(s);
        }
    }


}
