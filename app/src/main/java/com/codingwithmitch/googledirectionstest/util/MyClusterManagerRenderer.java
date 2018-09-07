package com.codingwithmitch.googledirectionstest.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.codingwithmitch.googledirectionstest.R;
import com.codingwithmitch.googledirectionstest.models.ClusterMarker;
import com.codingwithmitch.googledirectionstest.models.UserLocation;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyClusterManagerRenderer extends DefaultClusterRenderer<ClusterMarker>
{

    private static final String TAG = "MyClusterManagerRendere";

    private final IconGenerator iconGenerator;
    private final IconGenerator clusterIconGenerator;
    private final ImageView imageView;
    private final ImageView clusterImageView;
    private final int markerWidth;
    private final int markerHeight;
    private Context context;

    public MyClusterManagerRenderer(Context context, GoogleMap googleMap,
                                    ClusterManager<ClusterMarker> clusterManager) {

        super(context, googleMap, clusterManager);

        this.context = context.getApplicationContext();

        // initialize cluster icon generator
        clusterIconGenerator = new IconGenerator(context.getApplicationContext());
        View clusterView = LayoutInflater.from(context).inflate(R.layout.layout_custom_marker, null);
        clusterIconGenerator.setContentView(clusterView);
        clusterImageView = clusterView.findViewById(R.id.profile_image);

        // initialize cluster item icon generator
        iconGenerator = new IconGenerator(context.getApplicationContext());
        imageView = new ImageView(context.getApplicationContext());
        markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        imageView.setLayoutParams(new ViewGroup.LayoutParams(markerWidth, markerHeight));
        int padding = (int) context.getResources().getDimension(R.dimen.custom_marker_padding);
        imageView.setPadding(padding, padding, padding, padding);
        iconGenerator.setContentView(imageView);

    }

    @Override
    protected void onBeforeClusterItemRendered(ClusterMarker item, MarkerOptions markerOptions) {

        imageView.setImageResource(item.getIconPicture());
        Bitmap icon = iconGenerator.makeIcon();
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon)).title(item.getTitle());

    }

    @Override
    protected void onBeforeClusterRendered(Cluster<ClusterMarker> cluster, MarkerOptions markerOptions) {

        Iterator<ClusterMarker> iterator = cluster.getItems().iterator();

        clusterImageView.setImageResource(iterator.next().getIconPicture());
        Bitmap icon = clusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
    }

    public void setUpdateMarker(ClusterMarker clusterMarker) {
        Marker marker = getMarker(clusterMarker);
        if (marker != null) {
            marker.setPosition(clusterMarker.getPosition());
        }
    }

    @Override
    protected boolean shouldRenderAsCluster(Cluster cluster) {
        return cluster.getSize() > 4;
    }
}

















