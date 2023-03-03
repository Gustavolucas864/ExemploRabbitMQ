package br.ufs.dcomp.ExemploRabbitMQ;

import com.rabbitmq.client.*;
import java.util.Scanner;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import com.rabbitmq.client.Delivery;
import java.util.Arrays;


public class Chat {

  private final static String QUEUE_NAME = "minha-fila";
  private static String username;
  private static String message = "";                                           // mensagem escrita pelo usuario
  private static String addressee = "";                                         // destinatario (usuario)
  private static String flag = "";                                              // se eh @ ou # (se eh para usuario ou grupo)
  private static String prompt = "";                                            // nome do destinatario 
  private static String exchangeName = "";                                      // destinatario (grupo)
  private static String usernameAdd = "";                                       // usuario que sera adicionado ou removido de um grupo (precisa ser uma variavel global msm?)
  private static String exchangeType = "fanout";                                // 
  private static boolean durable = true ;
  
  
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.237.91.173"); // IP DA MÁQUINA VIRTUAL 
    factory.setUsername("admin"); // USUÁRIO
    factory.setPassword("password"); // SENHA
    factory.setVirtualHost("/");    
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    Scanner sc = new Scanner(System.in);
    
    // DEFINICAO DO USUARIO EMISSOR
    while(true){
      System.out.print("User: ");
      username = sc.nextLine();
      if(!username.isEmpty() && !username.contains(" ") && !username.startsWith("@") && !username.startsWith("!") &&
         !username.startsWith("#")){
        break;
      }
      else{
        System.out.println("Erro: nome de usuario invalido");
      }
    }
    String queue = username;
    System.out.println("Bem Vindo " + username + " !");
    System.out.println("Ajuda: Digite o @ do seu destinatario antes de enviar alguma mensagem.");
    System.out.println("Ajuda: Digite /stop para cancelar o chat.");
    System.out.println("Ajuda: Digite !comandos para verificar os comandos do chat.\n");
    
    channel.queueDeclare(queue, true, false, false, null);
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    // RECEBIMENTO DAS MENSAGENS
    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        if(flag.equals("@")){
          //System.out.println("Mensagem enviada para usuários.");
          
          String message = new String(body, "UTF-8");
          String[] mensagemDividida = message.split(":");
          String nome = mensagemDividida[0];
          
          int indiceDoisPontos = message.indexOf(":");
          String texto = message.substring(indiceDoisPontos + 2);
          
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");  
          ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Brazil/East"));
          String[] array_data_hora = dtf.format(now).split(" ");
          
          System.out.println("");
          System.out.println("(" + array_data_hora[0] + " às " + array_data_hora[1] + ") " + nome + " diz: " + texto);
          System.out.print("@" + nome + ">>");
        } else{
          System.out.println("Mensagem enviada para grupo.");
          
          String message = new String(body, "UTF-8");
          String[] mensagemDividida = message.split(":");
          String nome_grupo = mensagemDividida[0];
          
          int indiceDoisPontos = message.indexOf(":");
          String texto = message.substring(indiceDoisPontos + 2);
          
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");  
          ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Brazil/East"));
          String[] array_data_hora = dtf.format(now).split(" ");
          
          System.out.println("");
          System.out.println("(" + array_data_hora[0] + " às " + array_data_hora[1] + ") " + nome_grupo + " diz: " + texto);
          
          System.out.print("#" + exchangeName + ">>");
        }
      }
    };
    channel.basicConsume(queue, true, consumer);
    
    // ESCRITA DA MENSAGENS E DEFINICAO/ALTERACOES DO DESTINATARIO
    while(true){
      if(prompt.equals("")) {
        System.out.print(">>");
      } else{
        if(flag.equals("@")){
          System.out.print("@" + prompt + ">>");
        } else {
          System.out.print("#" + prompt + ">>");
        }
      }
      
      message = sc.nextLine();
      
      if (message.startsWith("@")) {
        if (message.contains(" ") || message.length()==1 || message.contains("/")){
          System.out.println("Erro: Destinatario invalido.");
        }
        else{
          addressee = message.substring(1);
          prompt = addressee;
          flag = "@";
          System.out.println("Enviando mensagens para: " + addressee);
        }
        
      } else if (message.startsWith("#")) {
        if (message.contains(" ") || message.length()==1 || message.contains("/")){
          System.out.println("Erro: Destinatario invalido.");
        }
        else{
          exchangeName = message.substring(1);
          prompt = exchangeName;
          flag = "#";
          System.out.println("Enviando mensagens para: " + exchangeName);
        }
        
      } else if (message.equals("/stop")){
        System.out.println("Encerrando o Chat...");
        break;
      } else if (message.startsWith("!")) {
        if(message.startsWith("!addGroup")){
         //System.out.println("O comando para criação de grupo foi disparado.");
         String [] arrayMsgSeparada = message.split(" ");
         if ((arrayMsgSeparada.length < 2) || (arrayMsgSeparada.length > 2)) { //evita erro por ter espacos dms ou nao ter todos os parametros do comando
            System.out.println("Comando inválido. Digite: !addGroup <nome do grupo>");
         } else{
            exchangeName = arrayMsgSeparada[1];
            if(!exchangeName.isEmpty() && !exchangeName.contains(" ") && !exchangeName.startsWith("@") && !exchangeName.startsWith("!") &&
            !exchangeName.contains("#")){
              channel.exchangeDeclare(exchangeName, exchangeType, durable);
              channel.queueBind(username, exchangeName, "");
              System.out.println("Grupo " + exchangeName + " criado com sucesso.");
            }else{
              System.out.println("Erro ao criar o grupo, tente novamente."); 
              System.out.println("O nome do grupo não pode ser vazio, não pode conter espaços vazios e não pode começar os caracteres: !, @, #.");
            }
         }
        } else if (message.startsWith("!addUser")) {
          System.out.println("O comando para adição de usuário do grupo foi disparado.");
           String [] arrayMsgSeparada = message.split(" ");
           
           if (arrayMsgSeparada.length < 3) {
              System.out.println("Comando inválido. Digite: !addUser <nome do usuário> <nome do grupo>");
           }else{
              usernameAdd = arrayMsgSeparada[1];
              exchangeName = arrayMsgSeparada[2];
             
              if(!usernameAdd.isEmpty() && !exchangeName.isEmpty() && !exchangeName.contains(" ") && !exchangeName.startsWith("@") && !exchangeName.startsWith("!") &&
              !exchangeName.startsWith("#")){
                channel.exchangeDeclare(exchangeName, exchangeType, durable);
                channel.queueBind(usernameAdd, exchangeName, "");
                System.out.println("Usuário " + usernameAdd + " adicionado ao grupo " + exchangeName + " com sucesso.");
              }else{
                System.out.println("Erro ao adicionar usuário ao grupo, tente novamente."); 
                System.out.println("O nome do usuário e do grupo não podem ser vazios, não podem conter espaços vazios e não podem começar os caracteres: !, @, #.");
              }
           }
        } else if (message.startsWith("!delFromGroup")) {
           System.out.println("O comando para exclusão de um usuário do grupo foi disparado.");
           String [] arrayMsgSeparada = message.split(" ");
           
           if (arrayMsgSeparada.length < 3) {
              System.out.println("Comando inválido. Digite: !delFromGroup <nome do usuário> <nome do grupo>");
           }else{
              usernameAdd = arrayMsgSeparada[1];
              exchangeName = arrayMsgSeparada[2];
             
              if(!usernameAdd.isEmpty() && !exchangeName.isEmpty() && !exchangeName.contains(" ") && !exchangeName.startsWith("@") && !exchangeName.startsWith("!") &&
              !exchangeName.startsWith("#")){
                channel.queueUnbind(usernameAdd, exchangeName, "");
                System.out.println("Usuário " + usernameAdd + " removido do grupo " + exchangeName + " com sucesso.");
              }else{
                System.out.println("Erro ao deletar o usuário do grupo, tente novamente."); 
                System.out.println("O nome do grupo e o nome do usuário não podem ser vazios, não podem conter espaços vazios e não podem começar os caracteres: !, @, #.");
              }
           }
        } else if (message.startsWith("!removeGroup")) {  //FALTA FAZER UNBIND DE TODOS OS USUARIOS ANTES DE REMOVER O GRUPO
           System.out.println("O comando para exclusão de grupo foi disparado.");
           String [] arrayMsgSeparada = message.split(" ");
         if (arrayMsgSeparada.length < 2) {
            System.out.println("Comando inválido. Digite: !removeGroup <nome do grupo>");
         }else{
            exchangeName = arrayMsgSeparada[1];
            if(!exchangeName.isEmpty() && !exchangeName.contains(" ") && !exchangeName.startsWith("@") && !exchangeName.startsWith("!") &&
            !exchangeName.startsWith("#")){
              channel.exchangeDelete(exchangeName);
              System.out.println("Grupo " + exchangeName + " deletado com sucesso.");
            }else{
              System.out.println("Erro deletar o grupo, tente novamente."); 
              System.out.println("O nome do grupo não pode ser vazio, não pode conter espaços vazios e não pode começar os caracteres: !, @, #.");
            }
         }
        } else if (message.equals("!comandos")) {
           System.out.println("O comando !addGroup <nome do grupo> serve para criar um novo grupo.");
           System.out.println("O comando !addUser <nome do usuário> <nome do grupo> serve para adicionar um usuário a um grupo.");
           System.out.println("O comando !delFromGroup <nome do usuário> <nome do grupo> serve para excluir um usuário de um grupo.");
           System.out.println("O comando !removeGroup <nome do grupo> serve para excluir um grupo.\n");
        } else {
           System.out.println("Comando inválido, digite !comandos e verifique os comandos possíveis.");
        }
      } else if (!addressee.isEmpty() || !exchangeName.isEmpty()) {
        if (flag.equals("@")){ 
          channel.basicPublish("", addressee, null, (username + ": " + message).getBytes("UTF-8"));
        } else {
          channel.basicPublish(exchangeName, "", null, (username + "#" + exchangeName + ": " + message).getBytes("UTF-8"));
        }
      } else {
        System.out.println("Erro: nao possui destinatario.");
      }
    }
    
    channel.close();
    connection.close();
  }
}





//147?
//21?


//etapa 3 vao ser 2 exchanges, uma para texto e outra para arquivo, ent vai ter 2 funcoes handleDelivery e vai ter q paralelizar o processo com thread