package com.example.qianwang.realmpractice.map;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.qianwang.realmpractice.R;
import com.example.qianwang.realmpractice.model.Photo;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterItem;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

/**
 * Created by qianwang on 6/30/16.
 */
    public class MapCluster extends BaseDemoActivity implements ClusterManager.OnClusterClickListener<MyItem>, ClusterManager.OnClusterInfoWindowClickListener<MyItem>, ClusterManager.OnClusterItemClickListener<MyItem>, ClusterManager.OnClusterItemInfoWindowClickListener<MyItem> {

    private RealmResults<Photo> results;
    private ClusterManager<MyItem> mClusterManager;

    @Override
    protected void startDemo() {
        mClusterManager = new ClusterManager<MyItem>(this, getMap());
        // Personalized the Renderer
        mClusterManager.setRenderer(new PhotoRenderer());
        getMap().setOnCameraChangeListener(mClusterManager);
        getMap().setOnMarkerClickListener(mClusterManager);
        getMap().setOnInfoWindowClickListener(mClusterManager);
        mClusterManager.setOnClusterClickListener(this);
        mClusterManager.setOnClusterInfoWindowClickListener(this);
        mClusterManager.setOnClusterItemClickListener(this);
        mClusterManager.setOnClusterItemInfoWindowClickListener(this);
        // Specify the Realm Database
        final RealmConfiguration config = new RealmConfiguration.Builder(this).deleteRealmIfMigrationNeeded()
                .build();
        Realm realm = Realm.getInstance(config);
        results = realm.where(Photo.class).findAll();
        addItems();
    }

    private void addItems() {
        for (Photo photo : results) {
            LatLng place = new LatLng(photo.getLatitude(), photo.getLongitude());
            //getMap().addMarker(new MarkerOptions().position(place));
            double lat = place.latitude;
            double lng = place.longitude;
            // Add ten cluster items in close proximity, for purposes of this example.
                MyItem offsetItem = new MyItem(lat,lng,photo.getId());
                mClusterManager.addItem(offsetItem);
        }
    }
    private class PhotoRenderer extends DefaultClusterRenderer<MyItem> {
        private final IconGenerator mClusterIconGenerator = new IconGenerator(getApplicationContext());
        private final IconGenerator mIconGenerator = new IconGenerator(getApplicationContext());
        private final ImageView mClusterImageView;
        private final int mDimension;
        private final ImageView mImageView;
        public PhotoRenderer(){
            super(getApplicationContext(), getMap(), mClusterManager);
            View multiProfile = getLayoutInflater().inflate(R.layout.multi_profile,null);
            mClusterIconGenerator.setContentView(multiProfile);
            mClusterImageView = (ImageView) multiProfile.findViewById(R.id.image);

            mImageView = new ImageView(getApplicationContext());
            mDimension = (int) getResources().getDimension(R.dimen.custom_profile_image);
            mImageView.setLayoutParams(new ViewGroup.LayoutParams(mDimension, mDimension));
            int padding = (int) getResources().getDimension(R.dimen.custom_profile_padding);
            mImageView.setPadding(padding, padding, padding, padding);
            mIconGenerator.setContentView(mImageView);
        }
        @Override
        protected void onBeforeClusterItemRendered(MyItem photo, MarkerOptions options){
            // Show a single photo, set the info photo to show their path
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Pictures/"+photo.fileName;
            File imgFile = new  File(filePath);
            if(imgFile.exists()){
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                mImageView.setImageBitmap(myBitmap);
            }
            Bitmap icon = mIconGenerator.makeIcon();
            options.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }
        @Override
        protected void onBeforeClusterRendered(Cluster<MyItem> cluster, MarkerOptions markerOptions) {
            // Draw multiple people.
            // Note: this method runs on the UI thread. Don't spend too much time in here (like in this example).
            List<Drawable> profilePhotos = new ArrayList<Drawable>(Math.min(4, cluster.getSize()));
            int width = mDimension;
            int height = mDimension;

            for (MyItem p : cluster.getItems()) {
                // Draw 4 at most.
                if (profilePhotos.size() == 4) break;
                String filePath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/Pictures/"+p.fileName;
                File imgFile = new  File(filePath);
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                Drawable drawable = new BitmapDrawable(getResources(),myBitmap);
                drawable.setBounds(0, 0, width, height);
                profilePhotos.add(drawable);
            }
            MultiDrawable multiDrawable = new MultiDrawable(profilePhotos);
            multiDrawable.setBounds(0, 0, width, height);
            mClusterImageView.setImageDrawable(multiDrawable);
            Bitmap icon = mClusterIconGenerator.makeIcon(String.valueOf(cluster.getSize()));
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }
        @Override
        protected boolean shouldRenderAsCluster(Cluster cluster) {
            // Always render clusters.
            return cluster.getSize() > 0;
        }
    }

    @Override
    public boolean onClusterClick(Cluster<MyItem> cluster) {
        // Create the builder to collect all essential cluster items for the bounds.
        LatLngBounds.Builder builder = LatLngBounds.builder();
        for (ClusterItem item : cluster.getItems()) {
            builder.include(item.getPosition());
        }
        // Get the LatLngBounds
        final LatLngBounds bounds = builder.build();
        // Animate camera to the bounds,make the view to the new bounds
        try {
            getMap().animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onClusterInfoWindowClick(Cluster<MyItem> cluster) {
        // Does nothing, but you could go to a list of the users.
    }

    @Override
    public boolean onClusterItemClick(MyItem item) {
        // Does nothing, but you could go into the user's profile page, for example.
        return false;
    }

    @Override
    public void onClusterItemInfoWindowClick(MyItem item) {
        // Does nothing, but you could go into the user's profile page, for example.
    }

}

