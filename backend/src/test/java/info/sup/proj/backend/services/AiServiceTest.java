package info.sup.proj.backend.services;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import info.sup.proj.backend.config.AzureOpenAiConfig;
import info.sup.proj.backend.model.Puzzle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiServiceTest {

    @Mock
    private OpenAIClient openAIClient;
    
    @Mock
    private AzureOpenAiConfig azureOpenAiConfig;
    
    private AiService aiService;
    
    @BeforeEach
    void setUp() {
        // Only stubbing what we actually use
        when(azureOpenAiConfig.getDeploymentName()).thenReturn("mock-deployment");
        
        // Create a subclass of AiService with overridden createOpenAIClient method
        aiService = new AiService(azureOpenAiConfig) {
            @Override
            protected OpenAIClient createOpenAIClient(AzureOpenAiConfig config) {
                return openAIClient; // Return our mock instead
            }
        };
    }
    
    @Test
    void testGenerateResponse_whenRequestIsTooGeneral() {
        // Arrange
        String userInput = "Help me implement everything";
        String currentCode = "public class Example {}";
        Puzzle.Type puzzleType = Puzzle.Type.BY_PASS;
        
        // Mock the request analysis to return "BROAD"
        ChatCompletions analyzeCompletions = mock(ChatCompletions.class);
        ChatResponseMessage analyzeResponseMessage = mock(ChatResponseMessage.class);
        when(analyzeResponseMessage.getContent()).thenReturn("BROAD");
        
        ChatChoice analyzeChoice = mock(ChatChoice.class);
        when(analyzeChoice.getMessage()).thenReturn(analyzeResponseMessage);
        
        when(analyzeCompletions.getChoices()).thenReturn(Arrays.asList(analyzeChoice));
        
        // Set up the mock to return our analysis
        when(openAIClient.getChatCompletions(eq("mock-deployment"), any(ChatCompletionsOptions.class)))
            .thenReturn(analyzeCompletions);
        
        // Act
        AiService.ChatResponse response = aiService.generateResponse(userInput, currentCode, puzzleType);
        
        // Assert
        assertEquals("BROAD", response.getText());
        assertEquals("", response.getCode());
        
        // Verify the call to Azure was made once for analysis but not for generating a solution
        verify(openAIClient, times(1)).getChatCompletions(
            eq("mock-deployment"),
            any(ChatCompletionsOptions.class)
        );
    }
    
    @Test
    void testGenerateResponse_whenRequestIsForCompleteSolution() {
        // Arrange
        String userInput = "Give me the solution";
        String currentCode = "public class Example {}";
        Puzzle.Type puzzleType = Puzzle.Type.FAULTY;
        
        // Mock the request analysis to return "SOLUTION"
        ChatCompletions analyzeCompletions = mock(ChatCompletions.class);
        ChatResponseMessage analyzeResponseMessage = mock(ChatResponseMessage.class);
        when(analyzeResponseMessage.getContent()).thenReturn("SOLUTION");
        
        ChatChoice analyzeChoice = mock(ChatChoice.class);
        when(analyzeChoice.getMessage()).thenReturn(analyzeResponseMessage);
        
        when(analyzeCompletions.getChoices()).thenReturn(Arrays.asList(analyzeChoice));
        
        // Set up the mock to return our analysis
        when(openAIClient.getChatCompletions(eq("mock-deployment"), any(ChatCompletionsOptions.class)))
            .thenReturn(analyzeCompletions);
        
        // Act
        AiService.ChatResponse response = aiService.generateResponse(userInput, currentCode, puzzleType);
        
        // Assert
        assertEquals("SOLUTION", response.getText());
        assertEquals("", response.getCode());
        
        // Verify the call to Azure was made once for analysis but not for generating a solution
        verify(openAIClient, times(1)).getChatCompletions(
            eq("mock-deployment"),
            any(ChatCompletionsOptions.class)
        );
    }
    
    @Test
    void testGetCodeEvaluation() {
        // Arrange
        String evaluationPrompt = "Evaluate this code to solve a puzzle";
        String code = "public class Solution { public void solve() {} }";
        Puzzle.Type puzzleType = Puzzle.Type.MULTI_STEP;
        
        // Mock the chat completions response with a JSON evaluation score
        String mockResponse = "{\"correctness\": 85, \"quality\": 78}";
        
        ChatCompletions evalCompletions = mock(ChatCompletions.class);
        ChatResponseMessage evalResponseMessage = mock(ChatResponseMessage.class);
        when(evalResponseMessage.getContent()).thenReturn(mockResponse);
        
        ChatChoice evalChoice = mock(ChatChoice.class);
        when(evalChoice.getMessage()).thenReturn(evalResponseMessage);
        
        when(evalCompletions.getChoices()).thenReturn(Arrays.asList(evalChoice));
        
        // Set up the mock to return our evaluation
        when(openAIClient.getChatCompletions(eq("mock-deployment"), any(ChatCompletionsOptions.class)))
            .thenReturn(evalCompletions);
        
        // Act
        String evaluation = aiService.getCodeEvaluation(evaluationPrompt, code, puzzleType);
        
        // Assert
        assertEquals(mockResponse, evaluation);
        
        // Verify the call to Azure was made with the right parameters
        ArgumentCaptor<ChatCompletionsOptions> optionsCaptor = ArgumentCaptor.forClass(ChatCompletionsOptions.class);
        verify(openAIClient).getChatCompletions(eq("mock-deployment"), optionsCaptor.capture());
        
        // Check that the evaluation prompt was included and has the code attached
        ChatCompletionsOptions capturedOptions = optionsCaptor.getValue();
        boolean hasEvaluationPrompt = false;
        for (ChatRequestMessage message : capturedOptions.getMessages()) {
            if (message instanceof ChatRequestUserMessage) {
                String content = ((ChatRequestUserMessage) message).getContent().toString();
                if (content.contains(evaluationPrompt) && content.contains(code)) {
                    hasEvaluationPrompt = true;
                    break;
                }
            }
        }
        assertTrue(hasEvaluationPrompt);
    }
}