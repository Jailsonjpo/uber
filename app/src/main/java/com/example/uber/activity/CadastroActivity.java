package com.example.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.example.uber.R;
import com.example.uber.config.ConfiguracaoFirebase;
import com.example.uber.helper.UsuarioFirebase;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import com.example.uber.model.Usuario;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoNome, campoEmail, campoSenha;
    private Switch switchTipoUsuario;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        //inicializar componentes

        campoNome = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.editCadastroSenha);
        switchTipoUsuario = findViewById(R.id.switchTipoUsuario);

    }

    public void validarCadastroUsuario(View view){
//Recuperar Textos dos campos
        String textoNome = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if(!textoNome.isEmpty()){ // verifica o Nome

            if(!textoEmail.isEmpty()){ //Verifica e-mail

                if(!textoSenha.isEmpty()){ //Verifica Senha

                    Usuario usuario = new Usuario();

                    usuario.setNome( textoNome);
                    usuario.setEmail( textoEmail);
                    usuario.setSenha(textoSenha);
                    usuario.setTipo(verificaTipoUsuario());

                    cadastrarUsuario(usuario);
                }else{

                    Toast.makeText(CadastroActivity.this, "Preencha a Senha", Toast.LENGTH_LONG).show();

                }

            }else{

                Toast.makeText(CadastroActivity.this, "Preencha o Email!", Toast.LENGTH_LONG).show();

            }

        }else{

            Toast.makeText(CadastroActivity.this, "Preencha o nome!", Toast.LENGTH_LONG).show();

        }

    }

    public void cadastrarUsuario(final Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

               if(task.isSuccessful()){

                try{

                    String idUsuario = task.getResult().getUser().getUid();
                    usuario.setId(idUsuario);
                    usuario.salvar();

                    //Atualizar nome do UserProfile

                    UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                    //Redireciona o uruario co base no seu tipo
                    // Se o usuário for passageiro chama a activity maps
                    //senão chama a activity requisicoes

                    if(verificaTipoUsuario() == "P"){

                        startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                        finish();

                        Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Passageiro!", Toast.LENGTH_LONG).show();
                    }else{
                        startActivity(new Intent(CadastroActivity.this, RequisicoesActivity.class));
                        finish();

                        Toast.makeText(CadastroActivity.this, "Sucesso ao cadastrar Motorista!", Toast.LENGTH_LONG).show();

                    }

                }catch (Exception e){
                    e.printStackTrace();
                   }

               }else{

                        String excecao = "";
                        try{
                            throw task.getException();
                        }catch (FirebaseAuthWeakPasswordException e){
                            excecao = "Digite uma senha mais forte!";
                        }catch (FirebaseAuthInvalidCredentialsException e){
                            excecao = "Por favor, digite um e-mail válido";
                        }catch(FirebaseAuthUserCollisionException e){
                            excecao = "Esta conta já foi cadastrada";
                        }catch (Exception e){
                            excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                            e.printStackTrace();
                        }

                   Toast.makeText(CadastroActivity.this, excecao, Toast.LENGTH_LONG).show();

               }

            }
        });

    }

    public String verificaTipoUsuario(){

        return switchTipoUsuario.isChecked() ? "M" : "P";

    }

}
