package tech.tensordash.tensordash.view.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import tech.tensordash.tensordash.R;
import tech.tensordash.tensordash.service.model.Project;
import tech.tensordash.tensordash.view.adapter.ProjectAdapter;
import tech.tensordash.tensordash.viewmodel.FirebaseAuthViewModel;
import tech.tensordash.tensordash.viewmodel.FirebaseAuthViewModelFactory;
import tech.tensordash.tensordash.viewmodel.FirebaseDatabaseViewModel;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessaging;

public class DashboardActivity extends AppCompatActivity {

    private static final String TAG = "DashboardActivity";
    FirebaseDatabaseViewModel databaseViewModel;
    FirebaseAuthViewModel firebaseAuthViewModel;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerView;
    private TextView noProjectsPresentTextView;
    private ProjectAdapter projectAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        noProjectsPresentTextView = findViewById(R.id.no_projects_present_textview);

        projectAdapter = new ProjectAdapter();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(projectAdapter);

        setAreProjectsPresentProgressBar(false);



        firebaseAuthViewModel = ViewModelProviders.of(DashboardActivity.this, new FirebaseAuthViewModelFactory(getApplication(), DashboardActivity.this)).get(FirebaseAuthViewModel.class);

        databaseViewModel = ViewModelProviders.of(DashboardActivity.this).get(FirebaseDatabaseViewModel.class);
        databaseViewModel.getAllProjects().observe(this, projects -> {
            setAreProjectsPresentProgressBar(!projects.isEmpty());
            projectAdapter.submitList(projects);
        });

        projectAdapter.setOnItemClickListener(project -> {
            Intent intent = new Intent(DashboardActivity.this, ProjectDescriptionActivity.class);
            intent.putExtra("project_name", project.getProjectName());
            startActivityForResult(intent, 0);
        });

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            databaseViewModel.refreshProjectList(swipeRefreshLayout);
            databaseViewModel.getAllProjects().observe(this, projects -> {
                setAreProjectsPresentProgressBar(!projects.isEmpty());
                projectAdapter.submitList(projects);
                projectAdapter.notifyDataSetChanged();
            });

        });

        noProjectsPresentTextView.setOnClickListener(v -> openDocumentation());

        FirebaseMessaging.getInstance().subscribeToTopic(FirebaseAuth.getInstance().getUid());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_signout) {
            AlertDialog.Builder builder = new AlertDialog.Builder(DashboardActivity.this);
            builder.setTitle("Sign out?")
                    .setMessage("Do you want to sign out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        signOut();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                    })
                    .create()
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOut() {
        // TODO: Add a listener (Analytics event)
        firebaseAuthViewModel.signOut();
        databaseViewModel.detachListeners();
        startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
        finishAffinity();
    }

    private void setAreProjectsPresentProgressBar(boolean areProjectsPresent){
        Log.d(TAG, "setAreProjectsPresentProgressBar: " + areProjectsPresent);
        if(areProjectsPresent){
            recyclerView.setVisibility(View.VISIBLE);
            noProjectsPresentTextView.setVisibility(View.GONE);
        }else{
            recyclerView.setVisibility(View.GONE);
            noProjectsPresentTextView.setVisibility(View.VISIBLE);
        }
    }

    private void openDocumentation(){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/CleanPegasus/TensorDash/blob/master/README.md"));
        startActivity(browserIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == ProjectDescriptionActivity.DELETE_PROJECT){
            deleteProject(data.getStringExtra("delete_project"));
        }
    }

    public void deleteProject(String deleteProjectName){
        databaseViewModel.getAllProjects().observe(this, projects -> {
            for(int i = 0; i < projects.size(); i++){
                if(projects.get(i).getProjectName().equals(deleteProjectName)){
                    projects.remove(i);
                    break;
                }
            }
            projectAdapter.submitList(projects);
            projectAdapter.notifyDataSetChanged();
            databaseViewModel.deleteProject(deleteProjectName);

        });
    }
}
