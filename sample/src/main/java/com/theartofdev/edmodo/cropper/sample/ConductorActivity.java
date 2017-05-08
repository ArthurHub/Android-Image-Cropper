package com.theartofdev.edmodo.cropper.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.Conductor;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.RouterTransaction;
import com.example.croppersample.R;

public class ConductorActivity extends AppCompatActivity {

    private Router router;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conductor);

        router = Conductor.attachRouter(this, (ViewGroup)findViewById(R.id.container), savedInstanceState);
        router.setRoot(RouterTransaction.with(new ConductorController()));

    }
}
