package fm.client;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.joanzapata.iconify.IconDrawable;
import com.joanzapata.iconify.fonts.FontAwesomeIcons;

import java.util.ArrayList;
import java.util.List;

import Model.Event;
import Model.Person;
import java.util.Comparator;


public class MapFragment extends Fragment implements OnMapReadyCallback {
    private final List<Polyline> polylines = new ArrayList<Polyline>();
    public static final String ARG_TITLE = "title";
    private GoogleMap map;
    String eventID = null;
    View view;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        eventID = getArguments().getString("EVENT");
        if (eventID == null) {
            setHasOptionsMenu(true);
        }
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(layoutInflater, container, savedInstanceState);
        view = layoutInflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        TextView textview = (TextView) view.findViewById(R.id.mapTextView);


        return view;
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        super.onCreateOptionsMenu(menu,menuInflater);
        menuInflater.inflate(R.menu.main_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {

        switch(menu.getItemId()) {
            case R.id.searchItem:
                Intent intent = new Intent(getContext(), SearchActivity.class);
                startActivity(intent);
                return true;
            case R.id.settingsItem:
                intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(menu);
        }
    }
    @Override
    public void onMapReady(GoogleMap googleMap) {

        map = googleMap;
        DataCache data = DataCache.getInstance();
        ArrayList<String> eventstypes = new ArrayList<>();
        ArrayList<Float> colors = new ArrayList<>();
        ArrayList<Event> events = new ArrayList<>();
        events = data.getUserEvents();
        if (data.getFathersSide()){
            events.addAll(data.setFatherEvents());
        }
        if (data.getMothersSide()){
            events.addAll(data.setMotherEvents());
        }
        if (!data.getMaleEvents() && !data.getFemaleEvents()){
            events.clear();
        }
        else if (data.getMaleEvents() && !data.getFemaleEvents()){
            events = data.setMaleEvents(events);
        }
        else if (!data.getMaleEvents() && data.getFemaleEvents()){
            events = data.setFemaleEvents(events);
        }
        data.setEvents(events);

        // Add a marker in Sydney and move the camera
        for (Event e : data.getEvents()) {
            LatLng sydney = new LatLng(e.getLatitude(), e.getLongitude());
            Marker m = null;
            int check = -1;
            for (int i = 0; i < eventstypes.size(); ++i) {
                if (e.geteventType().equals(eventstypes.get(i))){
                    check = i;
                }
            }
            if (check == -1) {
                eventstypes.add(e.geteventType());
                Float color = (float)colors.size() * 60;
                while (color >= 360){
                    color = color / 2;
                }
                m = map.addMarker(new MarkerOptions().position(sydney).title(e.geteventID()).icon(BitmapDescriptorFactory
                        .defaultMarker(color)));
                colors.add(color);
            }
            else {
                m = map.addMarker(new MarkerOptions().position(sydney).title(e.geteventID()).icon(BitmapDescriptorFactory
                        .defaultMarker(colors.get(check))));
            }
            m.setTag(e);
            //Life Story Line

            //Family Tree Line
            if (events.contains(data.getUserEvents().get(0))) {
                sydney = new LatLng(data.getUserEvents().get(0).getLatitude(), data.getUserEvents().get(0).getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLng(sydney));
            }
            else {
                sydney = new LatLng(events.get(0).getLatitude(), events.get(0).getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLng(sydney));
            }

            e = null;
            for (Event event : data.getEvents()){
                if (event.geteventID().equals(eventID)){
                    e = event;
                }
            }
            if (e != null){
                LatLng event = new LatLng(e.getLatitude(), e.getLongitude());
                map.animateCamera(CameraUpdateFactory.newLatLng(event));
                TextView textview = (TextView) view.findViewById(R.id.mapTextView);
                Person g = null;
                for (Person p :data.getPeople()) {
                    if (p.getPersonID().equals(e.getPersonID())){
                        g = p;
                    }
                }
                textview.setText(g.getFirstName() + " " + g.getLastName() + "\n" + e.geteventType() + ": " + e.getCity() + ", " + e.getCountry() + ", " + e.getYear() );
            }
        }

        map.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public boolean onMarkerClick(Marker m) {
                for(Polyline line : polylines) {
                    line.remove();
                }
                polylines.clear();
                TextView textview = (TextView) view.findViewById(R.id.mapTextView);
                Event e = (Event) m.getTag();
                Person g = null;
                for (Person p :data.getPeople()) {
                    if (p.getPersonID().equals(e.getPersonID())){
                        g = p;
                    }
                }
                if (g != null) {
                    textview.setText(g.getFirstName() + " " + g.getLastName() + "\n" + e.geteventType() + ": " + e.getCity() + ", " + e.getCountry() + ", " + e.getYear() );
                    Person finalG = g;
                    LatLng sydney = new LatLng(e.getLatitude(), e.getLongitude());
                    map.animateCamera(CameraUpdateFactory.newLatLng(sydney));
                    textview.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(getContext(), PersonActivity.class);
                            intent.putExtra("THIS_PERSON", finalG.getPersonID());
                            startActivity(intent);
                        }
                    });
                }
                if (data.getSpouseLines()) {
                    //Spouse Line
                    if (g.getSpouseID() != null) {
                        LatLng sydney = new LatLng(e.getLatitude(), e.getLongitude());
                        Person s = null;
                        for (Person p : data.getPeople()) {
                            if (g.getSpouseID().equals(p.getPersonID())) {
                                s = p;
                            }
                        }
                        Event j = null;
                        ArrayList<Event> events = new ArrayList<>();
                        for (Event f : data.getEvents()) {
                            if (f.getPersonID().equals(s.getPersonID())) {
                                events.add(f);
                            }
                        }

                        for (Event d : events) {
                            if (j == null) {
                                j = d;
                            }
                            if (j.getYear() > d.getYear()) {
                                j = d;
                            }
                        }
                        if (j != null) {
                            LatLng spouse = new LatLng(j.getLatitude(), j.getLongitude());
                            Polyline p = map.addPolyline(new PolylineOptions()
                                    .add(sydney, spouse)
                                    .width(5)
                                    .color(Color.BLUE));

                            polylines.add(p);
                        }
                        events.clear();
                    }
                }
                if (data.getLifeStoryLines()) {
                    //Life Story Lines
                    Event j = null;
                    ArrayList<Event> events = new ArrayList<>();
                    for (Event f : data.getEvents()) {
                        if (f.getPersonID().equals(g.getPersonID())) {
                            events.add(f);
                        }
                    }
                    Comparator<Event> EventSorter = new Comparator<Event>() {
                        @Override
                        public int compare(Event o1, Event o2) {
                            return Integer.compare(o1.getYear(), o2.getYear());
                        }
                    };
                    events.sort(EventSorter);
                    for (int i = 0; i < events.size() - 1; ++i) {
                        LatLng current = new LatLng(events.get(i).getLatitude(), events.get(i).getLongitude());
                        LatLng next = new LatLng(events.get(i + 1).getLatitude(), events.get(i + 1).getLongitude());
                        Polyline p = map.addPolyline(new PolylineOptions()
                                .add(current, next)
                                .width(5)
                                .color(Color.GREEN));
                        polylines.add(p);
                    }
                    events.clear();
                }
                if (data.getFamilyTreeLines()) {
                    //family tree lines
                    familyTreeLineHelper(g, e, 16);
                }
                return true;
            }
        });
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void familyTreeLineHelper(Person g, Event e, int num){
        if (num <= 0){
            num = 1;
        }
        DataCache data = DataCache.getInstance();
        if (g.getFatherID() != null) {
            Person f = null;
            for (Person p : data.getPeople()) {
                if (g.getFatherID().equals(p.getPersonID())) {
                    f = p;
                }
            }
            Event j = null;
            ArrayList<Event> events = new ArrayList<>();
            for (Event d : data.getEvents()) {
                if (d.getPersonID().equals(f.getPersonID())) {
                    events.add(d);
                }
            }
            if (events.size() > 0) {
                for (Event event : events) {
                    if (event.geteventType().equals("birth")) {
                        j = event;
                    }
                }
                if (j == null) {
                    Comparator<Event> EventSorter = new Comparator<Event>() {
                        @Override
                        public int compare(Event o1, Event o2) {
                            return Integer.compare(o1.getYear(), o2.getYear());
                        }
                    };
                    events.sort(EventSorter);
                    j = events.get(0);
                }

                LatLng current = new LatLng(e.getLatitude(), e.getLongitude());
                LatLng next = new LatLng(j.getLatitude(), j.getLongitude());
                Polyline p = map.addPolyline(new PolylineOptions()
                        .add(current, next)
                        .width(num)
                        .color(Color.BLACK));
                polylines.add(p);
                familyTreeLineHelper(f, j, num - 4);
            }
        }
        if (g.getMotherID() != null) {
            Person m = null;
            for (Person p : data.getPeople()) {
                if (g.getMotherID().equals(p.getPersonID())) {
                    m = p;
                }
            }
            Event j = null;
            ArrayList<Event> events = new ArrayList<>();
            for (Event d : data.getEvents()) {
                if (d.getPersonID().equals(m.getPersonID())) {
                    events.add(d);
                }
            }
            for (Event event : events){
                if (event.geteventType().equals("birth")){
                    j = event;
                }
            }
            if (events.size() > 0) {
                if (j == null) {
                    Comparator<Event> EventSorter = new Comparator<Event>() {
                        @Override
                        public int compare(Event o1, Event o2) {
                            return Integer.compare(o1.getYear(), o2.getYear());
                        }
                    };
                    events.sort(EventSorter);
                    j = events.get(0);
                }
                LatLng current = new LatLng(e.getLatitude(), e.getLongitude());
                LatLng next = new LatLng(j.getLatitude(), j.getLongitude());
                Polyline p = map.addPolyline(new PolylineOptions()
                        .add(current, next)
                        .width(num)
                        .color(Color.RED));
                polylines.add(p);
                familyTreeLineHelper(m, j, num - 4);
            }
        }
    }
}
