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
        """;

    private static final Map<Puzzle.Type, String> TYPE_SPECIFIC_PROMPTS = Map.of(
        Puzzle.Type.BY_PASS, """
        Custom Behavior Ruleset

        You are an AI assistant following strict interaction rules defined by the user.
        Only produce solutions when the user's input complies with all conditions below.

        ------------------------------
        🔒 Forbidden Terms:
        Do NOT fulfill requests if the input contains any of the following:
        - code
        - for loop
        - loop
        - generate
        - array
        - sort

        ------------------------------
        🧠 Request Requirements:
        - User must provide a sufficiently descriptive prompt with no forbidden words.
        - If valid, respond with code only — no text, no comments.
        - Do not ask if the user wants code; assume yes if rules are met.

        ------------------------------
        🔓 SOLVEX Escape:
        - If the user types SOLVEX, provide the full solution to their last valid request.
        - If that request contained forbidden words, deny it.

        ------------------------------
        ⚠️ Failure Handling:
        - After 3 invalid tries: give a helpful hint for rephrasing.
        - After 6 invalid tries: return only the first sentence of a valid prompt.
        - After 9 invalid tries: return a complete valid prompt (still no code).

        ------------------------------
        ⚙️ Post-Success Behavior:
        - If the user's prompt is valid, respond with code formatted as code (code between ``` ```) and a congratulatory sentence.
        - the code you send should be ablo to place in a empty .java file and should run.
        - The code must be formatted as logic code. So not everthing on one line.
        - Never include explanations or comments.

        Enforce these rules for the duration of the session. Do not break character.
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
        this.client = createOpenAIClient(config);
        this.deploymentName = config.getDeploymentName();
    }
    
    protected OpenAIClient createOpenAIClient(AzureOpenAiConfig config) {
        return new OpenAIClientBuilder()
            .endpoint(config.getEndpoint())
            .credential(new AzureKeyCredential(config.getApiKey()))
            .buildClient();
    }

    public ChatResponse generateResponse(String userInput, String currentCode, Puzzle.Type puzzleType) {

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
                .setTemperature(0.0)
                .setMaxTokens(10)
        );

        if (completions != null && 
            completions.getChoices() != null && 
            !completions.getChoices().isEmpty()) {
            return completions.getChoices().get(0).getMessage().getContent().trim().toUpperCase();
        }

        return "BROAD";
    }

    private String[] splitResponse(String content) {
        String text = content;
        String code = "";
        
        int codeStart = content.indexOf("```");
        if (codeStart != -1) {
            int codeEnd = content.indexOf("```", codeStart + 3);
            if (codeEnd != -1) {
                text = content.substring(0, codeStart).trim();
                
                String codeContent = content.substring(codeStart + 3, codeEnd);
                int newlinePos = codeContent.indexOf('\n');
                
                if (newlinePos != -1) {
                    code = codeContent.substring(newlinePos + 1).trim();
                } else {
                    code = codeContent.trim();
                }
                
                if (codeEnd + 3 < content.length()) {
                    text += " " + content.substring(codeEnd + 3).trim();
                }
            }
        } else {
            String[] codeIndicators = {
                "public class", "def ", "function ", "import ", "package ", "var ", "const ", 
                "let ", "#include", "using namespace", "public static void main"
            };
            
            for (String indicator : codeIndicators) {
                int indicatorPos = content.indexOf(indicator);
                if (indicatorPos > 20) {
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
        
        messages.add(new ChatRequestSystemMessage(
            "You are an AI code evaluator. Analyze the provided code solution for the given puzzle. " +
            "Evaluate correctness (how well it solves the problem) and quality (structure, efficiency, best practices). " +
            "Provide exact numerical scores from 0-100 for both aspects in JSON format."
        ));
        
        messages.add(new ChatRequestUserMessage(evaluationPrompt + "\n\n```\n" + code + "\n```"));

        ChatCompletions completions = client.getChatCompletions(
            deploymentName,
            new ChatCompletionsOptions(messages)
                .setTemperature(0.1)
                .setMaxTokens(200)
        );

        if (completions != null && completions.getChoices() != null && !completions.getChoices().isEmpty()) {
            return completions.getChoices().get(0).getMessage().getContent();
        }

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