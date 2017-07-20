package org.naturenet.ui;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.naturenet.data.model.Users;

import java.util.ArrayList;


public class FetchData {
    //intialize Singleton instance
    private static final FetchData ourInstance = new FetchData();

    //initialize ArrayList of users
    private ArrayList<Users> users;

    /**
     * This function queries our database for all Users.
     * @return ArrayList of User objects from our Firebase Database.
     */
    private static ArrayList<Users> queryUsers(){

        final ArrayList<Users> users = new ArrayList<>();

        //Query our database for all the users and order them by display name.
        FirebaseDatabase.getInstance().getReference().child(Users.NODE_NAME).orderByKey().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user: dataSnapshot.getChildren()){
                    users.add(user.getValue(Users.class));
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        return users;
    }

    /**
     *
     * @return Singleton instance.
     */
    public static synchronized FetchData getInstance() {
        return ourInstance;
    }

    private FetchData() {
        this.users = queryUsers();
    }

    public ArrayList<Users> getUsers(){
        return this.users;
    }
}
