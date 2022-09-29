package com.blmhmt.artbook;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.blmhmt.artbook.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    private ActivityArtBinding binding;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ActivityResultLauncher<String> permissionLauncher;
    Bitmap selectedImage;
    SQLiteDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        registerLauncher();
        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null); //varsa acar , yoksa oluşturur

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        if (info.equals("new")) {
            binding.save.setVisibility(View.VISIBLE);
        } else {
            int artId = intent.getIntExtra("id", 0);
            binding.save.setVisibility(View.GONE);
            binding.yearText.setEnabled(false);

            try {
                Cursor cursor = database.rawQuery("SELECT * FROM arts WHERE id=?", new String[]{String.valueOf(artId)});

                int artNameIx = cursor.getColumnIndex("artname");
                int painterNameIx = cursor.getColumnIndex("paintername");
                int yearIx = cursor.getColumnIndex("year");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()) {
                    binding.editName.setText(cursor.getString(artNameIx));
                    binding.artsitText.setText(cursor.getString(painterNameIx));
                    binding.yearText.setText(cursor.getString(yearIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    public void save(View view) {

        String name = binding.editName.getText().toString();
        String artistName = binding.artsitText.getText().toString();
        String year = binding.yearText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage, 300);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        try {

            //oluşturulmadıysa tabl oluştur diyip değerleri yazıyoruz
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR, paintername VARCHAR, year VARCHAR, image BLOB)");

            String sqlString = "INSERT INTO arts(artname, paintername, year, image) VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1, name);
            sqLiteStatement.bindString(2, artistName);
            sqLiteStatement.bindString(3, year);
            sqLiteStatement.bindBlob(4, byteArray);
            sqLiteStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(ArtActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

    }

    //sqlite görsel sınırı mevcut , resmi kücültmemiz lazım
    public Bitmap makeSmallerImage(Bitmap image, int size) {
        int width = image.getWidth();
        int height = image.getHeight();
        float bitmapRatio = (float) width / (float) height; //bitmapin oranı

        if (bitmapRatio > 1) {
            width = size;
            height = (int) (width / bitmapRatio);
        } else {
            height = size;
            width = (int) (height * bitmapRatio);
        }

        return image.createScaledBitmap(image, width, height, true);
    }


    public void selectImage(View view) {

        //izin kontrolü
        //packagemanager = içerik sağlayan yardımcı bir sınıf
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            //izin icin acıklama kısmı , android kendisi kontrol ediyor
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Snackbar.make(binding.getRoot(), "Permission needed for gallery.", Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }
                }).show();
            } else {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            activityResultLauncher.launch(intentToGallery);
        }

    }


    //izin ve resim alma işlemleri
    public void registerLauncher() {

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                //bir sey secildi mi kontrolü
                if (result.getResultCode() == RESULT_OK) {
                    Intent intent = result.getData();
                    if (intent != null) {
                        Uri uri = intent.getData();
                        //binding.imageView.setImageURI(uri); adres degil veri lazım
                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                ImageDecoder.Source source = ImageDecoder.createSource(getContentResolver(), uri);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            } else {
                                selectedImage = MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(), uri);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result) {
                    Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                } else {
                    Toast.makeText(ArtActivity.this, "Permission needed", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}