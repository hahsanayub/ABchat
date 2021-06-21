package com.aapkabazzaar.abchat;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.browse.MediaBrowser;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String chat_user_id, chat_user_name, current_user_id;
    private Toolbar mChatToolbar;
    private FirebaseAuth mAuth;
    private DatabaseReference mUsersRef, mRootRef;
    private TextView chatUserName, chatUserLastSeen;
    private CircleImageView chatUserImage;
    private EditText chatMessageEditText;
    private ImageButton chatMessageAddBtn, chatMessageSendBtn;
    private RecyclerView mMessageList;
    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager mLinearLayout;
    private MessageAdapter mAdapter;
    private LinearLayout mMessageLinearLayout;
    private boolean exist;
    private static final int TOTAL_ITEMS_TO_LOAD = 10;
    private static final int GALLERY_PICK = 1;
    private StorageReference mImageStorage;
    private RelativeLayout relativeLayout;
    private Snackbar snackbar;
    private ProgressDialog mDeleteProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        mDeleteProgress = new ProgressDialog(this);
        mMessageList = findViewById(R.id.message_list_recycler_view);

        new ItemTouchHelper(iTemCallBack).attachToRecyclerView(mMessageList);
        chat_user_id = getIntent().getStringExtra("user_id");
        chat_user_name = getIntent().getStringExtra("chatUserName");
        mChatToolbar = findViewById(R.id.chat_bar_layout);
        mImageStorage = FirebaseStorage.getInstance().getReference();

        setSupportActionBar(mChatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        relativeLayout = findViewById(R.id.relativeChatLayout);

        if (!isConnectedToInternet(this)) {
            showSnackBar("Please check your internet connection", relativeLayout);
        }

        mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            current_user_id = mAuth.getCurrentUser().getUid();
        }

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View cutom_view = inflater.inflate(R.layout.chat_custom_bar, null);
        actionBar.setCustomView(cutom_view);

        chatUserName = findViewById(R.id.custom_bar_display_name);
        chatUserLastSeen = findViewById(R.id.custom_bar_last_seen);
        chatUserImage = findViewById(R.id.custom_bar_image);
        if (chat_user_name.length() < 20) {
            chatUserName.setText(chat_user_name);
        } else {
            chat_user_name = chat_user_name.substring(0, 17) + "...";
            chatUserName.setText(chat_user_name);
        }

        mAdapter = new MessageAdapter(messagesList);
        mMessageLinearLayout = findViewById(R.id.chat_message_linear_layout);
        mLinearLayout = new LinearLayoutManager(this);
        mMessageList.setHasFixedSize(true);
        mMessageList.setLayoutManager(mLinearLayout);
        // mMessageList.getLayoutManager().setMeasurementCacheEnabled(false);
        mMessageList.setAdapter(mAdapter);


        mRootRef = FirebaseDatabase.getInstance().getReference();
        mUsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        mRootRef.keepSynced(true);
        mUsersRef.keepSynced(true);
        loadMessages();
        setSeenMessages();
        mRootRef.child("Friends").child(current_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //  if (dataSnapshot.child(chat_user_id).exists())
                //  {

                mMessageLinearLayout.setVisibility(View.INVISIBLE);
                int optionId = dataSnapshot.child(chat_user_id).exists() ? R.layout.send_message : R.layout.unable_to_send_message;

                View C = findViewById(R.id.chat_message_linear_layout);
                ViewGroup parent = (ViewGroup) C.getParent();
                int index = parent.indexOfChild(C);
                parent.removeView(C);
                C = getLayoutInflater().inflate(optionId, parent, false);
                parent.addView(C, index);

                if (optionId == R.layout.send_message) {
                    chatMessageEditText = C.findViewById(R.id.chat_message_edit_text);
                    chatMessageAddBtn = C.findViewById(R.id.chat_message_add_btn);
                    chatMessageSendBtn = C.findViewById(R.id.chat_message_send_btn);

                    chatMessageSendBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sendMessage();
                            setSeenMessages();

                        }
                    });


                    chatMessageAddBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent galleryIntent = new Intent();
                            galleryIntent.setType("image/*");
                            galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                            startActivityForResult(Intent.createChooser(galleryIntent, "Select Image"), GALLERY_PICK);


                        }
                    });
                }
                //  }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mUsersRef.child(chat_user_id).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String thumbImage = dataSnapshot.child("thumb_image").getValue().toString();
                Picasso.with(ChatActivity.this).load(thumbImage).placeholder(R.drawable.default_avatar2).into(chatUserImage);
                String online = dataSnapshot.child("online").getValue().toString();

                if (online.equals("true")) {
                    chatUserLastSeen.setText("Online");
                } else {
                    GetTimeAgo getTimeAgo = new GetTimeAgo();
                    long last_seen = Long.parseLong(online);
                    String last_seen_time = getTimeAgo.getTimeAgo(last_seen, getApplicationContext());
                    chatUserLastSeen.setText(last_seen_time);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private boolean isConnectedToInternet(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void showSnackBar(String message, RelativeLayout relativeLayout) {
        snackbar = Snackbar
                .make(relativeLayout, message, Snackbar.LENGTH_INDEFINITE).
                        setAction("Ok", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                snackbar.dismiss();
                            }
                        });
        snackbar.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();

            final String currentUserRef = "messages/" + current_user_id + "/" + chat_user_id;
            final String chatUserRef = "messages/" + chat_user_id + "/" + current_user_id;

            final String currentUserMessageRef = "lastMessage/" + current_user_id + "/" + chat_user_id;
            final String chatUserMessageRef = "lastMessage/" + chat_user_id + "/" + current_user_id;

            DatabaseReference messageUserRef = mRootRef.child("messages").child(current_user_id).child(chat_user_id).push();
            final String pushId = messageUserRef.getKey();

            StorageReference filePath = mImageStorage.child("message_images").child(pushId + ".jpg");

            filePath.putFile(imageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {

                    if (task.isSuccessful()) {
                        String downloadUrl = task.getResult().getDownloadUrl().toString();

                        Map messageMap = new HashMap();
                        messageMap.put("message", downloadUrl);
                        messageMap.put("seen", false);
                        messageMap.put("type", "image");
                        messageMap.put("time", ServerValue.TIMESTAMP);
                        messageMap.put("from", current_user_id);

                        Map messageUserMap = new HashMap();
                        messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
                        messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

                        Map lastMessageMap = new HashMap();
                        lastMessageMap.put("lastMessageKey", pushId);

                        Map lastMessageUserMap = new HashMap();
                        lastMessageUserMap.put(currentUserMessageRef, lastMessageMap);
                        lastMessageUserMap.put(chatUserMessageRef, lastMessageMap);

                        chatMessageEditText.setText("");

                        mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                                } else {
                                    loadMessages();
                                }
                            }
                        });

                        mRootRef.updateChildren(lastMessageUserMap, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                if (databaseError != null) {
                                    Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    }
                }
            });


        }
    }

    public void setSeenMessages() {
        mRootRef.child("messages").child(current_user_id).child(chat_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    for (DataSnapshot seenData : dataSnapshot.getChildren()) {

                        seenData.getRef().child("seen").setValue(true);


                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void loadMessages() {
        //
        messagesList.clear();
        DatabaseReference messagesRef = mRootRef.child("messages").child(current_user_id).child(chat_user_id);
        messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);

                mAdapter.notifyDataSetChanged();

                mMessageList.scrollToPosition(messagesList.size() - 1);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        //  }

    }

    private void sendMessage() {
        mAdapter.notifyDataSetChanged();
        String message = chatMessageEditText.getText().toString();
        if (!TextUtils.isEmpty(message)) {
            String currentUserRef = "messages/" + current_user_id + "/" + chat_user_id;
            String chatUserRef = "messages/" + chat_user_id + "/" + current_user_id;

            String currentUserMessageRef = "lastMessage/" + current_user_id + "/" + chat_user_id;
            String chatUserMessageRef = "lastMessage/" + chat_user_id + "/" + current_user_id;

            DatabaseReference messageUserRef = mRootRef.child("messages").child(current_user_id).child(chat_user_id).push();
            String pushId = messageUserRef.getKey();

            Map messageMap = new HashMap();
            messageMap.put("message", message);
            messageMap.put("seen", false);
            messageMap.put("type", "text");
            messageMap.put("time", ServerValue.TIMESTAMP);
            messageMap.put("from", current_user_id);

            Map messageUserMap = new HashMap();
            messageUserMap.put(currentUserRef + "/" + pushId, messageMap);
            messageUserMap.put(chatUserRef + "/" + pushId, messageMap);

            Map lastMessageMap = new HashMap();
            lastMessageMap.put("lastMessageKey", pushId);

            Map lastMessageUserMap = new HashMap();
            lastMessageUserMap.put(currentUserMessageRef, lastMessageMap);
            lastMessageUserMap.put(chatUserMessageRef, lastMessageMap);

            chatMessageEditText.setText("");

            mRootRef.updateChildren(messageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    } else {
                        loadMessages();
                    }
                }
            });

            mRootRef.updateChildren(lastMessageUserMap, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError != null) {
                        Toast.makeText(ChatActivity.this, databaseError.getMessage().toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            sendToStart();
        } else {
            mUsersRef.child(currentUser.getUid()).child("online").setValue("true");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            mUsersRef.child(currentUser.getUid()).child("online").setValue(ServerValue.TIMESTAMP);
        }
    }

    private void sendToStart() {

        Intent startIntent = new Intent(ChatActivity.this, StartActivity.class);
        startActivity(startIntent);
        finish();
    }


    ItemTouchHelper.SimpleCallback iTemCallBack = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            messagesList.clear();
            mAdapter.notifyDataSetChanged();

            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, final int direction) {
            mDeleteProgress.setTitle("Deleting Message");
            mDeleteProgress.setMessage("Please wait , Message s being deleted ");
            mDeleteProgress.show();

            final int v = viewHolder.getAdapterPosition();

            final String txtMsg = ((TextView) mMessageList.findViewHolderForAdapterPosition(v).itemView.findViewById(R.id.message_single_text_view)).getText().toString();


            Query msgQuery = FirebaseDatabase.getInstance().getReference().child("messages").child(current_user_id).child(chat_user_id).orderByChild("message");


            msgQuery.equalTo(txtMsg).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot datanSnapshot) {
                    for (final DataSnapshot msgSnapshot : datanSnapshot.getChildren()) {
//                        final String msgId = msgSnapshot.getKey().toString();
                        Query lastMessage = FirebaseDatabase.getInstance().getReference().child("lastMessage")
                                .child(current_user_id).child(chat_user_id);
                        lastMessage.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                final String lastMsgKey = dataSnapshot.child("lastMessageKey").getValue().toString();
                                if (String.valueOf(msgSnapshot.getKey()).equals(lastMsgKey) && v == 0) {
                                    msgSnapshot.getRef().removeValue();
                                    mDeleteProgress.hide();
                                    loadMessages();
                                }
//
                                else if (String.valueOf(msgSnapshot.getKey()).equals(lastMsgKey)) {

                                    delPrevMsg(v);
                                    msgSnapshot.getRef().removeValue();
                                    mDeleteProgress.hide();
                                    loadMessages();


                                }
//
                                else {
                                    msgSnapshot.getRef().removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                loadMessages();
                                                mDeleteProgress.hide();
                                                Toast.makeText(getApplicationContext(), "Message Deleted Successfully", Toast.LENGTH_SHORT).show();
                                                if (task.getException() instanceof Exception) {
                                                    Toast.makeText(ChatActivity.this, "" + task.getException().getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }
                                    });
                                }

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });


                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    };

    public void delPrevMsg(int v) {

        final Query prevMsgQuery = FirebaseDatabase.getInstance().getReference().child("messages")
                .child(current_user_id).child(chat_user_id).orderByChild("message");
        final String prevTxtMsg = ((TextView) mMessageList.findViewHolderForAdapterPosition(v - 1)
                .itemView.findViewById(R.id.message_single_text_view)).getText().toString();

        prevMsgQuery.equalTo(prevTxtMsg).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot data_Snapshot) {
                for (final DataSnapshot prevMsgSnapshot : data_Snapshot.getChildren()) {
                    String test = prevMsgSnapshot.getKey().toString();

                    FirebaseDatabase.getInstance().getReference().child("lastMessage")
                            .child(current_user_id).child(chat_user_id).child("lastMessageKey").setValue(test);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

}
