package com.example.sleep_app;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import com.google.firebase.firestore.FirebaseFirestore;


import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.sleep_app.databinding.ActivityProfileBinding;
import com.example.sleep_app.model.Users;

public class ProfileActivity extends AppCompatActivity {

    ActivityProfileBinding binding;
    ProgressDialog progressDialog;
    private String userID;
    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        userID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();


        binding.logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(ProfileActivity.this);
                builder.setTitle("Logout");
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage("Are you sure you want to logout?");
                builder.setBackground(getResources().getDrawable(R.drawable.input_background, null));
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FirebaseAuth.getInstance().signOut();
                        Intent intent = new Intent(ProfileActivity.this, SplashScreenActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
                builder.setNegativeButton("No", null);
                builder.show();
            }
        });



        getUserDatafromFirebase();

    }


    private void getUserDatafromFirebase() {
        showProgressDialog();
        db.collection("Users").document(userID)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String fullname = documentSnapshot.getString("name");
                        String email_ = documentSnapshot.getString("email");
                        String photoUrl = documentSnapshot.getString("user_image");

                        binding.userName.setText(fullname);
                        binding.userEmail.setText(email_);

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Picasso.get().load(photoUrl).placeholder(R.drawable.profile).error(R.drawable.profile).into(binding.userImage);
                        }
                    }
                    progressDialog.dismiss();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Cannot fetch data", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                });

    }


    private void showProgressDialog() {
        progressDialog = new ProgressDialog(ProfileActivity.this);
        progressDialog.show();
        progressDialog.setContentView(R.layout.progress_dialog);
        progressDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(progressDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

    }


}