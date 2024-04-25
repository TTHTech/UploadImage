package com.example.firebaseapp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

public class ImageFragment extends Fragment {

    private RecyclerView recyclerView;
    private ArrayList<Image> arrayList;
    private ImageAdapter adapter;
    private ProgressDialog progressDialog;

    public ImageFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_image, container, false);
        recyclerView = view.findViewById(R.id.recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        arrayList = new ArrayList<>();
        adapter = new ImageAdapter(getContext(), arrayList);
        adapter.setOnItemClickListener(new ImageAdapter.OnItemClickListener() {
            @Override
            public void onClick(Image image) {
                showZoomedImage(image.getUrl());
            }
        });
        recyclerView.setAdapter(adapter);
        return view;
    }

    private void showZoomedImage(String imageUrl) {
        final Dialog zoomDialog = new Dialog(requireContext());
        zoomDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        zoomDialog.setContentView(R.layout.dialog_zoom_image);
        ImageView zoomImageView = zoomDialog.findViewById(R.id.zoom_image_view);
        ImageButton deleteButton = zoomDialog.findViewById(R.id.delete_button);

        // Sử dụng Glide để tải và hiển thị ảnh
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL); // Lưu cache để tăng tốc độ load ảnh
        Glide.with(requireContext())
                .load(imageUrl)
                .apply(options)
                .into(zoomImageView);

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteImageFromFirebase(imageUrl);
                zoomDialog.dismiss();
            }
        });
        zoomDialog.show();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseApp.initializeApp(requireContext());

        FloatingActionButton fabReload = view.findViewById(R.id.fab_reload);
        fabReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgressDialog();
                loadImagesFromFirebase();
            }
        });
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
    }

    private void loadImagesFromFirebase() {
        FirebaseStorage.getInstance().getReference().child("images").listAll().addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                arrayList.clear();
                if (listResult.getItems().isEmpty()) {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Không có ảnh được tải về", Toast.LENGTH_SHORT).show();
                } else {
                    for (StorageReference item : listResult.getItems()) {
                        Image image = new Image();
                        image.setName(item.getName());
                        item.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    String url = "https://" + task.getResult().getEncodedAuthority() + task.getResult().getEncodedPath() + "?alt=media&token=" + task.getResult().getQueryParameters("token").get(0);
                                    image.setUrl(url);
                                    arrayList.add(image);
                                    adapter.notifyDataSetChanged();
                                } else {
                                    Toast.makeText(requireContext(), "Failed to retrieve images", Toast.LENGTH_SHORT).show();
                                }
                                progressDialog.dismiss();
                            }
                        });
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(requireContext(), "Failed to retrieve images", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
        });
    }

    private void deleteImageFromFirebase(String imageUrl) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);

        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(requireContext(), "Ảnh đã được xóa", Toast.LENGTH_SHORT).show();
                int position = -1;
                for (int i = 0; i < arrayList.size(); i++) {
                    if (arrayList.get(i).getUrl().equals(imageUrl)) {
                        position = i;
                        break;
                    }
                }
                if (position != -1) {
                    arrayList.remove(position);
                    adapter.notifyItemRemoved(position);
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast.makeText(requireContext(), "Lỗi: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
