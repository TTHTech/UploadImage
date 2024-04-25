package com.example.firebaseapp;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.UUID;

public class UploadFragment extends Fragment {
    private ArrayList<Uri> arrayList;
    private TextView textView;
    private MaterialButton select, upload;

    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
        if (o.getResultCode() == getActivity().RESULT_OK) {
            handleResult(o.getData());
        }
    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_upload, container, false);

        FirebaseApp.initializeApp(getActivity());

        arrayList = new ArrayList<>();

        select = rootView.findViewById(R.id.selectImages);
        upload = rootView.findViewById(R.id.uploadImages);
        textView = rootView.findViewById(R.id.selectedTv);

        select.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            activityResultLauncher.launch(intent);
        });

        upload.setOnClickListener(view -> {
            upload.setText("Uploading images...");
            upload.setEnabled(false);
            uploadImages(arrayList);
        });

        return rootView;
    }

    private void handleResult(Intent data) {
        arrayList.clear();
        if (data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                // Single image selection
                arrayList.add(imageUri);
            } else {
                // Multiple image selection
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        Uri uri = data.getClipData().getItemAt(i).getUri();
                        arrayList.add(uri);
                    }
                }
            }
            if (!arrayList.isEmpty()) {
                upload.setEnabled(true);
                textView.setText(MessageFormat.format("{0} Images selected.", arrayList.size()));
            }
        }
    }

    private void uploadImages(@NonNull ArrayList<Uri> imageUriList) {
        LinearLayout progressLayout = getView().findViewById(R.id.progressLayout);
        for (int i = 0; i < imageUriList.size(); i++) {
            Uri uri = imageUriList.get(i);
            // Create a new layout for each image (Image + ProgressBar + Pause/Continue button)
            LinearLayout imageLayout = new LinearLayout(getActivity());
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(0, dpToPx(10), 0, 0); // Add top margin to separate progress layouts
            imageLayout.setLayoutParams(layoutParams);
            imageLayout.setOrientation(LinearLayout.HORIZONTAL);

            // ImageView for the image
            ImageView imageView = new ImageView(getActivity());
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dpToPx(60), dpToPx(60)); // Convert dp to pixels
            imageView.setLayoutParams(imageParams);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); // Scale type to maintain aspect ratio
            imageView.setImageURI(uri); // Set the image to be uploaded
            imageLayout.addView(imageView);

            // LinearLayout to contain ProgressBar and TextView
            LinearLayout progressTextLayout = new LinearLayout(getActivity());
            LinearLayout.LayoutParams progressTextLayoutParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            progressTextLayout.setLayoutParams(progressTextLayoutParams);
            progressTextLayout.setOrientation(LinearLayout.VERTICAL);

            // ProgressBar
            ProgressBar progressBar = new ProgressBar(getActivity(), null, android.R.attr.progressBarStyleHorizontal);
            LinearLayout.LayoutParams progressBarParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            progressBar.setLayoutParams(progressBarParams);
            progressBar.setMax(100);
            progressBar.setProgress(0);
            progressBarParams.setMargins(dpToPx(10), 0, dpToPx(10), 0); // Set margins for the ProgressBar
            progressTextLayout.addView(progressBar);

            TextView sizeText = new TextView(getActivity());
            LinearLayout.LayoutParams sizeTextParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sizeTextParams.gravity = Gravity.LEFT; // Align text to left
            sizeText.setLayoutParams(sizeTextParams);
            sizeText.setText("0B / 0B");
            sizeTextParams.leftMargin = dpToPx(10);
            progressTextLayout.addView(sizeText);

            // TextView to display progress percentage
            TextView progressText = new TextView(getActivity());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.gravity = Gravity.LEFT; // Align text to left
            progressText.setLayoutParams(textParams);
            progressText.setText("0%");
            textParams.leftMargin = dpToPx(10);
            progressTextLayout.addView(progressText);

            imageLayout.addView(progressTextLayout);

            // ImageButton for Pause
            ImageButton pauseImageButton = new ImageButton(getActivity());
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            pauseImageButton.setLayoutParams(buttonParams);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(40), dpToPx(40));
            pauseImageButton.setLayoutParams(params);
            pauseImageButton.setBackgroundColor(Color.WHITE);
            pauseImageButton.setImageResource(R.drawable.pause);
            pauseImageButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);// Set image resource for Pause button
            buttonParams.setMargins(dpToPx(10), 0, dpToPx(10), 0); // Set margins for the button
            imageLayout.addView(pauseImageButton);

            progressLayout.addView(imageLayout); // Add the imageLayout to the parent layout

            // Upload each image
            StorageReference storageReference = FirebaseStorage.getInstance().getReference("images").child(UUID.randomUUID().toString());
            UploadTask uploadTask = storageReference.putFile(uri);

            final int finalI = i; // Need to make the variable final to access inside the listener

            // Add click listener for Pause ImageButton
            pauseImageButton.setOnClickListener(view -> {
                if (pauseImageButton.getTag() == null || pauseImageButton.getTag().equals("pause")) {
                    // Handle pause functionality
                    uploadTask.pause(); // Pause the upload task
                    pauseImageButton.setImageResource(R.drawable.continute); // Change image resource to Continue icon
                    pauseImageButton.setTag("continue"); // Set tag to indicate the button state is Continue
                } else {
                    // Handle continue functionality
                    uploadTask.resume(); // Continue the upload task
                    pauseImageButton.setImageResource(R.drawable.pause); // Change image resource to Pause icon
                    pauseImageButton.setTag("pause"); // Set tag to indicate the button state is Pause
                }
            });

            // Add progress listener for each upload task
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progressBar.setProgress((int) progress);
                progressText.setText((int) progress + "%"); // Update progress text
                String uploadedSize = formatSize(taskSnapshot.getBytesTransferred()) + " / " + formatSize(taskSnapshot.getTotalByteCount());
                sizeText.setText(uploadedSize);
            }).addOnSuccessListener(taskSnapshot -> {
                // Handle success event if needed
                Toast.makeText(getActivity(), "Image " + (finalI + 1) + " uploaded successfully!", Toast.LENGTH_SHORT).show();
                // Change the image resource of the pauseImageButton to "Done"
                pauseImageButton.setImageResource(R.drawable.done);
                if (finalI == imageUriList.size() - 1) { // If it's the last image
                    upload.setText("Upload Images");
                    upload.setEnabled(true);
                }
            }).addOnFailureListener(e -> {
                // Handle failure event if needed
                Toast.makeText(getActivity(), "Failed to upload image " + (finalI + 1), Toast.LENGTH_SHORT).show();
                if (finalI == imageUriList.size() - 1) { // If it's the last image
                    upload.setText("Upload Images");
                    upload.setEnabled(true);
                }
            });
        }
    }

    private String formatSize(long bytes) {
        String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        return String.format("%.2f", size) + units[unitIndex];
    }

    // Helper method to convert dp to pixels
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }
}
