package com.example.kotlinmessenger

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.example.kotlinmessenger.messages.LatestMessageActivity
import com.example.kotlinmessenger.registerlogin.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
//import com.letsbuildthatapp.kotlinmessenger.LoginActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        register_button_register.setOnClickListener{
            performRegister()
        }
        already_have_account_text_view.setOnClickListener{
            Log.d("MainActivity","Try to show login activity")
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
        selectphoto_button_register.setOnClickListener {
            Log.d("MainActivity","Try to show photo selector")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent,0)
        }
    }

    var selectedPhotoUri: Uri? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode ==0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.d("MainActivity", "Photo was Selected")

            selectedPhotoUri = data.data //uri represents location where img is stored
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedPhotoUri)  //to access that img

            selectphoto_imageview_register.setImageBitmap(bitmap)

            selectphoto_button_register.alpha=0f

//            val bitmapDrawable = BitmapDrawable(bitmap)
//            selectphoto_button_register.setBackgroundDrawable(bitmapDrawable)

        }

    }


    private fun performRegister(){
        val email = email_edittext_register.text.toString()
        val password = password_edittext_register.text.toString()

        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this,"Please enter text in email/pw",Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity","Email is: "+email)
        Log.d("MainActivity","Password is: $password")


        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{
                if(!it.isSuccessful) {
                    Log.d("Main","In if loop")
                    return@addOnCompleteListener
                }
                else {
                    Log.d("Main", "Successfully created user with uid: ${it.result.user?.uid}")
                }

                uploadImageToFirebaseStorage()
            }
            .addOnFailureListener {
                Log.d("Main","Failed to create user: ${it.message}")
                Toast.makeText(this,"Failed to create user: ${it.message}",Toast.LENGTH_SHORT).show()
            }

    }
    private fun uploadImageToFirebaseStorage(){
//        Log.d("Main","in")
//        val imagee = Uri.parse("android.resource://${packageName}/drawable/test1")
//        val sttream = contentResolver.openInputStream(imagee)

        if(selectedPhotoUri == null) {
            Log.d("Main","Upload fail")
            return
        }
        else {
//        Log.d("Main","${sttream}")

        val filename = UUID.randomUUID().toString()
        val ref = FirebaseStorage.getInstance().getReference("/images/$filename")

            ref.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    Log.d("MainActivity", "Successfully uploaded image ${it.metadata?.path}")

                    ref.downloadUrl.addOnSuccessListener {
                        Log.d("MainActivity","File Location $it")

                        saveUserToFirebaseDatabase(it.toString())
                    }
                }
                .addOnFailureListener{
                    Log.d(TAG,"Failed to upload image to Storage : ${it.message}")
                }
        }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String){
        val uid = FirebaseAuth.getInstance().uid?:""
        val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")

        val user = User(uid,username_edittext_register.text.toString(),profileImageUrl)

        ref.setValue(user)
            .addOnSuccessListener {
                Log.d("MainActivity","Finally we saved the user to Firebase Database")

                val intent = Intent(this, LatestMessageActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK) //whenever he clicks back it goes to home screen instead of register page
                startActivity(intent)
            }

            .addOnFailureListener {
                Log.d(TAG,"Failed to set value to database:${it.message}")
            }
    }
}

class User(val uid:String,val username: String, val profileImageUrl: String){
    constructor() : this("","","")
}