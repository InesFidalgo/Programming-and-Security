
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;
import java.security.cert.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.Base64;

public class User implements Serializable
{  

    private String username           = null;
    private String password  = null;


    public User(String username, String password )
    {  
        this.username=username;
        this.password=password;

    }






    public void setUsername(String stuff){
        this.username =stuff;
    }
    public void setPassword( String stuff){
        this.password =stuff;
    }


    public String getUsername( ){
        return this.username ;
    }
    public String getPassword(  ){
        return this.password ;
    }

}