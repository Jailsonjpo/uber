package com.example.uber.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.example.uber.config.ConfiguracaoFirebase;
import com.example.uber.helper.UsuarioFirebase;
import com.example.uber.model.Destino;
import com.example.uber.model.Requisicao;
import com.example.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.location.LocationManagerCompat;

import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.uber.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PassageiroActivity extends AppCompatActivity implements OnMapReadyCallback {

    //componentes
    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;

    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private boolean uberChamado = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);

      inicializarComponentes();

      //Adiciona listener para status da requisição

        verificaStatusRequisicao();

    }

    /*
    Destino: -15.649341, -47.794018
    Lat/Lon passageiro: -15.649266, -47.791678
    Lat/Lon motorista a Caminho: inicial -15.648073, -47.789114
                       intermediaria -15.649733, -47.789506
                       final: -15.649320, -47.791609

     */

    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id").equalTo(usuarioLogado.getId());
        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for(DataSnapshot ds: dataSnapshot.getChildren()){

                        lista.add(ds.getValue(Requisicao.class));
                }

                Collections.reverse(lista);

                if(lista != null && lista.size() > 0){

                    requisicao = lista.get(0);

                    switch (requisicao.getStatus()){

                        case Requisicao.STATUS_AGUARDANDO :
                            linearLayoutDestino.setVisibility(View.GONE);
                            buttonChamarUber.setText("Cancelar Uber");
                            uberChamado = true;
                            break;

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recuperar Localizacao do usuário

        recuperarLocalizacaoUsuario();
    }
        public void chamarUber(View view){

            if(!uberChamado){ //Uber não foi chamado

            String enderecoDestino = editDestino.getText().toString();

                if(!enderecoDestino.equals("") || enderecoDestino != null){

                    Address addressDestino = recuperarEndereco(enderecoDestino);

                    if(addressDestino != null){

                        final Destino destino = new Destino();
                        destino.setCidade(addressDestino.getAdminArea());
                        destino.setCep(addressDestino.getPostalCode());
                        destino.setBairro(addressDestino.getSubLocality());
                        destino.setRua(addressDestino.getThoroughfare());
                        destino.setNumero(addressDestino.getFeatureName());
                        destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                        destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                        StringBuilder mensagem = new StringBuilder();
                        mensagem.append("Cidade: " + destino.getCidade());
                        mensagem.append("\nRua: " + destino.getRua());
                        mensagem.append("\nBairro: " + destino.getBairro());
                        mensagem.append("\nNúmero: " + destino.getNumero());
                        mensagem.append("\nCep: " + destino.getCep());

                        AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Confirme seu endereco!").setMessage(mensagem).setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                //Salvar Requisição

                                salvarRequisicao(destino);
                                uberChamado = true;

                            }
                        }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });

                        AlertDialog dialog = builder.create();
                        dialog.show();

                    }

                }else{

                    Toast.makeText(this,"Informe o endereço de destino!", Toast.LENGTH_SHORT).show();

                }//FIM CHAMAR UBER

            }else{
                    //CANCELAR A REQUISICAO

                uberChamado = false;

            }

            }

            private void salvarRequisicao(Destino destino){

                Requisicao requisicao = new Requisicao();
                requisicao.setDestino(destino);

                Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
                usuarioPassageiro.setLatitude( String.valueOf(localPassageiro.latitude));
                usuarioPassageiro.setLongitude( String.valueOf(localPassageiro.longitude));

                requisicao.setPassageiro(usuarioPassageiro);
                requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
                requisicao.salvar();

                linearLayoutDestino.setVisibility(View.GONE);
                buttonChamarUber.setText("Cancelar Uber");

            }


    private Address recuperarEndereco(String endereco){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if(listaEnderecos != null && listaEnderecos.size() > 0){
                Address address = listaEnderecos.get(0);
                return address;


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;

    }

        // Add a marker in Sydney and move the camera
       /* LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/


    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //recuperar latitude e longitude

                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar Geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(localPassageiro).title("Meu Local").icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
                );

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(localPassageiro, 15));

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        //Solicitar atualizações de Localização

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, locationListener);

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){

            case  R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;

        }
        return super.onOptionsItemSelected(item);
    }

    private void inicializarComponentes(){

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(toolbar);

        //Inicializar Componentes

        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarUber = findViewById(R.id.buttonChamarUber);


        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
}
