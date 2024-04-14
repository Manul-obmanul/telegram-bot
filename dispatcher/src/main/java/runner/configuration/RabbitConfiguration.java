package runner.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static model.RabbitQueue.*;

@Configuration
public class RabbitConfiguration {
    @Bean
    public MessageConverter jsonMessageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public Queue textMesageQueue(){
        return new Queue((TEXT_MESSAGE_UPDATE));
    }
    @Bean
    public Queue docMesageQueue(){
        return new Queue((DOC_MESSAGE_UPDATE));
    }
    @Bean
    public Queue photoMesageQueue(){
        return new Queue((PHOTO_MESSAGE_UPDATE));
    }
    @Bean
    public Queue answerMesageQueue(){
        return new Queue((ANSWER_MESSAGE));
    }
}
