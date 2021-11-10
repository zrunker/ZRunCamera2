package cc.ibooker.zruncamera2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import cc.ibooker.zruncamera2.camera.Camera2Activity;

public class MainActivity extends AppCompatActivity {
    private ImageView ivResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ivResult = findViewById(R.id.iv_result);

//        Intent intent = new Intent(this, Camera2Activity.class);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.parse(getTempFilePath()));
//        startActivity(intent);

        Intent intent = new Intent(this, Camera2Activity.class);
        startActivityForResult(intent, 1010);
    }

//    private String getTempFilePath() {
//        String filePath = null;
//        try {
//            filePath = this.getExternalCacheDir().getAbsolutePath() + File.separator + System.currentTimeMillis() + ".jpg";
//            File file = new File(filePath);
//            if (!file.exists() || !file.isFile()) {
//                file.createNewFile();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return filePath;
//    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 1010 && data != null) {
                String filePath = data.getStringExtra("filePath");
                Glide.with(this)
                        .load(filePath)
                        .into(ivResult);
            }
        }
    }
}