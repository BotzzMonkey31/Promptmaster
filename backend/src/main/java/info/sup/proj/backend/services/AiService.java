package info.sup.proj.backend.services;

import org.springframework.stereotype.Service;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import info.sup.proj.backend.config.AzureOpenAiConfig;
import info.sup.proj.backend.model.Puzzle;
import com.azure.ai.openai.models.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final OpenAIClient client;
    private final String deploymentName;
    
    private static final String REQUEST_ANALYZER_PROMPT = """
        You are an AI that analyzes user requests in the context of programming puzzles.
        You need to determine if a request is:
        1. Too general/broad
        2. Asking for a complete solution
        3. A specific, well-defined subtask
        
        Respond with exactly one of these words: BROAD, SOLUTION, or SPECIFIC
        Example responses:
        "Help me implement everything" -> BROAD
        "Give me the solution" -> SOLUTION
        "How to read from a file" -> SPECIFIC
        """;

    private static final Map<Puzzle.Type, String> TYPE_SPECIFIC_PROMPTS = Map.of(
        Puzzle.Type.BY_PASS, """
            You are an AI assistant helping with bypass challenges.
            Important guidelines:
            1. Never provide direct solutions or bypass methods
            2. Never suggest steps or approaches
            3. Only respond with guidance when specifically asked
            4. If users ask for complete solutions, respond with:
               "That's too much to handle in one go. Break this down into smaller steps."
            """,
        
        Puzzle.Type.Faulty, """
            You are an AI assistant helping with debugging challenges.
            Important guidelines:
            1. Never provide direct solutions or fix the code
            2. Never suggest what might be wrong
            3. Only respond to specific questions
            4. If users ask for complete solutions, respond with:
               "That's too much to handle in one go. Break this down into smaller steps."
            """,
        
        Puzzle.Type.Multi_Step, """
            You are an AI assistant helping with multi-step challenges.
            Guidelines:
            1. Never suggest steps or ways to break down the problem
            2. Never hint at what the next step should be
            3. When a user requests a specific, well-defined subtask:
               - Only then provide code for that specific subtask
               - Include explanatory comments in the code
            4. If the request is too broad or attempts multiple steps at once:
               - Respond with: "That's too much to handle in one go. Break this down into smaller steps."
            5. Never provide complete solutions all at once
            6. Never suggest what step should come next
            """
    );

    private static final String DEFAULT_SYSTEM_PROMPT = """
        You are an AI assistant helping with coding challenges.
        Important guidelines:
        1. Never suggest how to break down problems
        2. Only provide code for specific, well-defined subtasks when explicitly requested
        3. Never hint at next steps
        4. If users ask for complete solutions, respond with:
           "That's too much to handle in one go. Break this down into smaller steps."
        """;

    public AiService(AzureOpenAiConfig config) {
        this.client = new OpenAIClientBuilder()
            .endpoint(config.getEndpoint())
            .credential(new AzureKeyCredential(config.getApiKey()))
            .buildClient();
        this.deploymentName = config.getDeploymentName();
    }

    public ChatResponse generateResponse(String userInput, String currentCode, Puzzle.Type puzzleType) {
        // First, let the AI analyze the request
        String requestType = analyzeRequest(userInput);
        
        if ("BROAD".equals(requestType) || "SOLUTION".equals(requestType)) {
            return new ChatResponse(
                "That's too much to handle in one go. Break this down into smaller steps.",
                ""
            );
        }

        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(TYPE_SPECIFIC_PROMPTS.getOrDefault(puzzleType, DEFAULT_SYSTEM_PROMPT)));
        
        if (currentCode != null && !currentCode.trim().isEmpty()) {
            messages.add(new ChatRequestAssistantMessage("Current code context:\n" + currentCode));
        }
        
        messages.add(new ChatRequestUserMessage(userInput));

        ChatCompletions completions = client.getChatCompletions(
            deploymentName,
            new ChatCompletionsOptions(messages)
                .setTemperature(0.7)
                .setMaxTokens(800)
        );

        if (completions != null && completions.getChoices() != null && !completions.getChoices().isEmpty()) {
            String content = completions.getChoices().get(0).getMessage().getContent();
            String[] parts = splitResponse(content);
            return new ChatResponse(parts[0], parts[1]);
        }

        return new ChatResponse(
            "I couldn't generate a response. Please break down your request into smaller steps.",
            ""
        );
    }

    private String analyzeRequest(String input) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        messages.add(new ChatRequestSystemMessage(REQUEST_ANALYZER_PROMPT));
        messages.add(new ChatRequestUserMessage(input));

        ChatCompletions completions = client.getChatCompletions(
            deploymentName,
            new ChatCompletionsOptions(messages)
                .setTemperature(0.0) // Use 0 temperature for consistent analysis
                .setMaxTokens(10)    // We only need a single word response
        );

        if (completions != null && 
            completions.getChoices() != null && 
            !completions.getChoices().isEmpty()) {
            return completions.getChoices().get(0).getMessage().getContent().trim().toUpperCase();
        }

        // Default to treating as too broad if we can't analyze
        return "BROAD";
    }

    private String[] splitResponse(String content) {
        // Default to treating all content as text
        String text = content;
        String code = "";
        
        // Check for code blocks with triple backticks
        int codeStart = content.indexOf("```");
        if (codeStart != -1) {
            int codeEnd = content.indexOf("```", codeStart + 3);
            if (codeEnd != -1) {
                text = content.substring(0, codeStart).trim();
                
                // Get language identifier if present (e.g., ```python)
                String codeContent = content.substring(codeStart + 3, codeEnd);
                int newlinePos = codeContent.indexOf('\n');
                
                if (newlinePos != -1) {
                    // Remove language identifier if present
                    code = codeContent.substring(newlinePos + 1).trim();
                } else {
                    code = codeContent.trim();
                }
                
                // Check if there's text after the code block and append it to text
                if (codeEnd + 3 < content.length()) {
                    text += " " + content.substring(codeEnd + 3).trim();
                }
            }
        } else {
            // Alternative detection: Look for patterns that might indicate code without backticks
            // Common programming language patterns
            String[] codeIndicators = {
                "public class", "def ", "function ", "import ", "package ", "var ", "const ", 
                "let ", "#include", "using namespace", "public static void main"
            };
            
            for (String indicator : codeIndicators) {
                int indicatorPos = content.indexOf(indicator);
                if (indicatorPos > 20) {  // Avoid detecting indicators that are part of explanation text
                    text = content.substring(0, indicatorPos).trim();
                    code = content.substring(indicatorPos).trim();
                    break;
                }
            }
        }
        
        return new String[]{text, code};
    }

    public String getCodeEvaluation(String evaluationPrompt, String code, Puzzle.Type puzzleType) {
        List<ChatRequestMessage> messages = new ArrayList<>();
        
        // System message instructing the AI to evaluate code
        messages.add(new ChatRequestSystemMessage(
            "You are an AI code evaluator. Analyze the provided code solution for the given puzzle. " +
            "Evaluate correctness (how well it solves the problem) and quality (structure, efficiency, best practices). " +
            "Provide exact numerical scores from 0-100 for both aspects in JSON format."
        ));
        
        // Add the evaluation prompt that contains puzzle details
        messages.add(new ChatRequestUserMessage(evaluationPrompt + "\n\n```\n" + code + "\n```"));

        ChatCompletions completions = client.getChatCompletions(
            deploymentName,
            new ChatCompletionsOptions(messages)
                .setTemperature(0.1) // Low temperature for consistent evaluation
                .setMaxTokens(200)   // We just need the evaluation scores
        );

        if (completions != null && completions.getChoices() != null && !completions.getChoices().isEmpty()) {
            return completions.getChoices().get(0).getMessage().getContent();
        }

        // Return a default response if the AI evaluation fails
        return "{\"correctness\": 70, \"quality\": 70}";
    }

    public static class ChatResponse {
        private final String text;
        private final String code;

        public ChatResponse(String text, String code) {
            this.text = text;
            this.code = code;
        }

        public String getText() { return text; }
        public String getCode() { return code; }
    }
}