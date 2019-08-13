package com.alc.challenge.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    private EditText edtxtTitle, edtxtDesc, edtxtPrice;
    private ImageView imageView;
    private TravelDeal deal;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        edtxtTitle = findViewById(R.id.edtxtTitle);
        edtxtDesc = findViewById(R.id.edtxtDesc);
        edtxtPrice = findViewById(R.id.edtxtPrice);
        imageView = findViewById(R.id.imageView);
        Button btnUpload = findViewById(R.id.btnUpload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Insert Picture"), PICTURE_RESULT);
            }
        });
        Intent intent = getIntent();
        TravelDeal deal = (TravelDeal)  intent.getSerializableExtra("Deal");
        if(deal == null){
            deal = new TravelDeal();
        }
        this.deal = deal;
        edtxtTitle.setText(deal.getTitle());
        edtxtDesc.setText(deal.getDescription());
        edtxtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
    }

    private void saveDeal() {
        String title = edtxtTitle.getText().toString();
        String desc = edtxtDesc.getText().toString();
        String price = edtxtPrice.getText().toString();
        deal.setTitle(title);
        deal.setDescription(desc);
        deal.setPrice(price);
        if(deal.getId() == null){
            mDatabaseReference.push().setValue(deal);
        }else{
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }
    }

    private void deleteDeal(){
        if(deal == null){
            Toast.makeText(this, "Please save the deal before deleting", Toast.LENGTH_LONG).show();
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        if(deal.getImageName() != null && !deal.getImageName().isEmpty()){
            StorageReference picRef = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d("Delete Image", "Image deleted successfully");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("Delete Image", e.getMessage());
                }
            });
        }
    }
    private void backToList(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK){
            if(intent == null){
                Toast.makeText(this, "Invalid file!!! Please select another image", Toast.LENGTH_LONG).show();
                return;
            }
            Uri imageUri = intent.getData();
            if(imageUri == null || imageUri.getLastPathSegment() == null){
                Toast.makeText(this, "Invalid file!!! Please select another image", Toast.LENGTH_LONG).show();
                return;
            }
            StorageReference ref = FirebaseUtil.mStorageRef.child( imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                    //Uri snapshotUrl =  taskSnapshot.getDownloadUrl(); //Not Working
                    if (taskSnapshot.getMetadata() != null) {
                        if (taskSnapshot.getMetadata().getReference() != null) {
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();
                                    String imageName = taskSnapshot.getStorage().getPath();
                                    deal.setImageUrl(imageUrl);
                                    deal.setImageName(imageName);
                                    showImage(imageUrl);
                                }
                            });
                        }
                    }else{
                        Toast.makeText(DealActivity.this, "Invalid file!!! Please select another image", Toast.LENGTH_LONG).show();
                    }

                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if(FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditTexts(true);
        }else{
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditTexts(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save_menu:
                saveDeal();
                Toast.makeText(this, "Deal Saved", Toast.LENGTH_LONG).show();
                backToList();
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this, "Deal Deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableEditTexts(boolean isEnabled){
        edtxtTitle.setEnabled(isEnabled);
        edtxtDesc.setEnabled(isEnabled);
        edtxtPrice.setEnabled(isEnabled);
    }

    private void showImage(String url){
        if(url != null && !url.isEmpty()){
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get().load(url)
                    .resize(width, width * 2/3)
                    .centerCrop()
                    .into(imageView);
        }
    }

}
