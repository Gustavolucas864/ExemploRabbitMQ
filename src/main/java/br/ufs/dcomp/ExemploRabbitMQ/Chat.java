package br.ufs.dcomp.ExemploRabbitMQ;

import com.rabbitmq.client.*;
import java.util.Scanner;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import com.rabbitmq.client.Delivery;


public class Chat {

  private final static String QUEUE_NAME = "minha-fila";
  private static String username;
  private static String message = "";
  private static String addressee = "";
  private static String prompt = "";
  
  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("18.204.14.141"); // IP DA MÁQUINA VIRTUAL 
    factory.setUsername("admin"); // USUÁRIO
    factory.setPassword("password"); // SENHA
    factory.setVirtualHost("/");    
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();
    Scanner sc = new Scanner(System.in);
    
    // definicao do usuario emissor
    while(true){
      System.out.print("User: ");
      username = sc.nextLine();
      if(!username.isEmpty() && !username.contains(" ")){
        break;
      }
      else{
        System.out.println("Erro: nome de usuario invalido");
      }
    }
    String queue = username;
    System.out.println("Ajuda: Digite o @ do seu destinatario antes de enviar alguma mensagem.");
    System.out.println("Ajuda: Digite /stop para cancelar o chat.\n");
    
    channel.queueDeclare(queue, true, false, false, null);
    channel.queueDeclare(QUEUE_NAME, false,   false,     false,       null);
    
    Consumer consumer = new DefaultConsumer(channel) {
      @Override
      public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
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
      }
    };
    channel.basicConsume(queue, true, consumer);
    
    // escrita das mensagens e definicao/alteracoes do destinatario
    while(true){
      if(prompt.equals("")) {
        System.out.print(">>");
      } else{
        System.out.print("@" + prompt + ">>");
      }
    
      message = sc.nextLine();
      
      if (message.startsWith("@")) {
        if (message.contains(" ") || message.length()==1 || message.contains("/")){
          System.out.println("Erro: Destinatario invalido.");
        }
        else{
          addressee = message.substring(1);
          prompt = addressee;
          System.out.println("Enviando mensagens para: " + addressee);
          // System.out.println("@" + addressee + ">>");
        }
        
      } else if (message.equals("/stop")){
        System.out.println("Encerrando o Chat...");
        break;
      } else if (!addressee.isEmpty()) {
        channel.basicPublish("", addressee, null, (username + ": " + message).getBytes("UTF-8"));
      } else {
        System.out.println("Erro: nao possui destinatario.");
      }
    }
    channel.close();
    connection.close();
  }
}