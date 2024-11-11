package com.example.chatapplication

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference
    var receiverRoom: String? = null
    var senderRoom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser ?.uid

        mDbRef = FirebaseDatabase.getInstance().getReference()

        // Construct sender and receiver room identifiers
        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        supportActionBar?.title = name

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sentButton)
        messageList = ArrayList()
        messageAdapter = MessageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter

        // Load messages from Firebase using ChildEventListener
        mDbRef.child("chats").child(senderRoom!!).child("messages")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val message = snapshot.getValue(Message::class.java)
                    if (message != null) {
                        messageList.add(message)
                        messageAdapter.notifyItemInserted(messageList.size - 1)
                        chatRecyclerView.scrollToPosition(messageList.size - 1) // Scroll to the latest message
                        Log.d("ChatActivity", "Received message: ${message.message} from ${message.senderId}")
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle message updates if needed
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    // Handle message deletions if needed
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                    // Handle message moves if needed
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ChatActivity", "Database error: ${error.message}")
                }
            })

        // Send button click listener
        sendButton.setOnClickListener {
            val message = messageBox.text.toString()
            if (message.isNotEmpty()) {
                val messageObject = Message(message, senderUid)
                mDbRef.child("chats").child(senderRoom!!).child("messages").push().setValue(messageObject)
                    .addOnSuccessListener {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push().setValue(messageObject)
                        Log.d("ChatActivity", "Message sent: ${messageObject.message}")
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ChatActivity", "Failed to send message: ${exception.message}")
                    }
                messageBox.setText("")
            } else {
                Log.d("ChatActivity", "Empty message not sent")
            }
        }
    }
}