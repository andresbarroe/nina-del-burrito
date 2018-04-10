package com.example.zegerd.nina_del_burrito.vendor_activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zegerd.nina_del_burrito.R;
import com.example.zegerd.nina_del_burrito.classes.Item;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.util.List;

public class ViewFoodDetailsActivity extends AppCompatActivity {

    // Constant variables
    private static final int REQUEST_CAMERA = 3;
    private static final int SELECT_FILE = 2;

    // UI elements
    private TextView name, price, description;
    private CheckBox availabelBox;
    private ImageView foodImage;

    // FireBase vars
    private FirebaseAuth mAuth;
    private DatabaseReference itemRef;
    private StorageReference mStorageReference;

    // Temporal variables
    private Item currentItem;
    private String currentItemUrlPic;
    private Uri imageHoldUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_food_details);


        // Init UI elements
        name = findViewById(R.id.txtvw_detailName);
        price = findViewById(R.id.txtvw_detailPrice);
        description = findViewById(R.id.txtvw_detalDescription);
        availabelBox = findViewById(R.id.checkBox_detailAvailable);
        foodImage = findViewById(R.id.imageViewFood);

        // Get selected item
        Intent intent = getIntent();
        currentItem = (Item) intent.getSerializableExtra(VendorFoodActivity.FOOD_ITEM);
        currentItemUrlPic = currentItem.getItemPicture();

        // Init FireBase stuff
        mAuth = FirebaseAuth.getInstance();
        itemRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("ItemsAll")
                .child(mAuth.getCurrentUser().getUid() + currentItem.getNombre());
        mStorageReference = FirebaseStorage.getInstance()
                .getReference()
                .child("ItemPictures")
                .child(mAuth.getCurrentUser().getUid())
                .child(mAuth.getCurrentUser().getUid() + currentItem.getNombre());

        // Update UI elements
        name.setText(currentItem.getNombre());
        price.setText("" + currentItem.getPrecio());
        description.setText(currentItem.getDescripcion());
        availabelBox.setChecked(currentItem.isDisponible());

        foodImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Pick image
                pickFoodPicture();
            }
        });
    }

    private void pickFoodPicture() {
        // Display dialog to choose camera or gallery
        final CharSequence[] options = {"Take photo", "Choose from gallery", "Cancel"};
        AlertDialog.Builder builer = new AlertDialog.Builder(ViewFoodDetailsActivity.this);
        builer.setTitle("Add Photo");

        // Set listeners to each option
        builer.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (options[i].equals("Take photo")) {
                    cameraIntent();
                } else if (options[i].equals("Choose from gallery")) {
                    galleryIntent();
                } else if (options[i].equals("Cancel")) {
                    dialogInterface.dismiss();
                }
            }
        });
        builer.show();
    }

    private void galleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_FILE);
    }

    private void cameraIntent() {
        // Ask camera  permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);

        } else {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            cameraIntent();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == SELECT_FILE || requestCode == REQUEST_CAMERA) && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            CropImage.activity(imageUri)
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1, 1)
                    .start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                this.imageHoldUri = result.getUri();
                this.foodImage.setImageURI(imageHoldUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                // Toast of the error
            }
        }
    }

    public void deleteFood(View v) {
        // delete the register in db
        itemRef.setValue(null);

        Toast.makeText(this, "Producto eliminado de la tienda", Toast.LENGTH_SHORT).show();
        this.goBack();
    }

    public void updateFood(View v) {
        // update the register in db
        /*
        String nameItem = currentItem.getNombre();
        String descItem = currentItem.getDescripcion();
        float priceItem = currentItem.getPrecio();
        String vendorItem = currentItem.getVendorid();
        List<String> categItem = currentItem.getCategories();
        */
        //Item newItem = new Item(nameItem, descItem, priceItem, vendorItem, categItem);

        boolean availableItem = availabelBox.isChecked();
        currentItem.setDisponible(availableItem);

        if (imageHoldUri != null) {
            // Query to store the picture
            final String itemPicUrl = imageHoldUri.getLastPathSegment();
            StorageReference pictureItemRef = mStorageReference.child(itemPicUrl);
            Toast.makeText(this, "Entro", Toast.LENGTH_SHORT).show();

            pictureItemRef.putFile(imageHoldUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri imageUrl = taskSnapshot.getDownloadUrl();
                    currentItemUrlPic = imageUrl.toString();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(getApplicationContext(), "Error al subir imagen", Toast.LENGTH_SHORT).show();
                }
            });
        }
        currentItem.setItemPicture(currentItemUrlPic);
        itemRef.setValue(currentItem);
        Toast.makeText(this, "Producto modificado", Toast.LENGTH_SHORT).show();
        //this.goBack();
    }


    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        this.goBack();
    }

    private void goBack() {
        Intent result = new Intent();
        setResult(RESULT_OK, result);
        finish();
    }
}