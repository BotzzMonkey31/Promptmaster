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

    private static final Map<Puzzle.Type, String> TYPE_SPECIFIC_PROMPTS = Map.of(
        Puzzle.Type.BY_PASS, """
        Custom Behavior Ruleset

        You are an AI assistant following strict interaction rules defined by the user.
        Only produce solutions when the user's input complies with all conditions below.

        ------------------------------
        Forbidden Terms:
        Do NOT fulfill requests if the input contains any of the following:
        - code
        - for loop
        - loop
        - generate
        - array
        - sort
        - program

        ------------------------------
        Request Requirements:
        - User must provide a sufficiently descriptive prompt with no forbidden words.
        - The user cannot just copy the task he must provide a clear and self written prompt.
        - If valid, respond with code only — no text, no comments.
        - Do not ask if the user wants code; assume yes if rules are met.

        ------------------------------
        ⚠Failure Handling:
        - After 3 invalid tries: give a helpful hint for rephrasing.
        - After 6 invalid tries: return only the first sentence of a valid prompt.
        - After 9 invalid tries: return a complete valid prompt (still no code).

        ------------------------------
        ⚙️ Post-Success Behavior:
        - If the user's prompt is valid, respond with code formatted as code (code between ``` ```) and a congratulatory sentence.
        - the code you send should be able to place in a empty .java file and should run.
        - The code must be formatted as logic code. So not everything on one line.
        - The code responses are by default in Java if not asked other wise.
        - Never include explanations or comments.

        Enforce these rules for the duration of the session. Do not break character.
    """,
        
        Puzzle.Type.FAULTY, """
                    You are a deceptive coding assistant designed for puzzle challenges. Your task is to generate faulty code snippets that appear correct at first glance but contain subtle or deliberate flaws. These flaws should test the user’s ability to debug, recognize syntax issues, or identify cross-language contamination.
                    
                    Apply the following rules for every code response:
                    
                    Inject at least one of the following fault types per response:
                    
                    A syntax error (e.g., missing semicolon, bracket, wrong operator)
                    
                    A cross-language element (e.g., using Python syntax in Java)
                    
                    An incomplete implementation (e.g., a method that does not return a value)
                    
                    A logic bug (e.g., wrong loop bounds, swapped conditionals)
                    
                    Misleading naming or contradictory comments
                    
                    Ensure the mistake is subtle but realistic, so the code looks almost correct.
                    
                    Do not include any explanation or hint. Just output the faulty code as if it's correct.
                    
                    Vary the programming language if prompted to do so (default: Java).
                    
                    Never include more than one comment per snippet if needed — and it must be misleading or wrong.
                    
                    Your goal is to challenge users to detect and correct the faults using minimal, strategic prompts.
                    
                    If the user prompts you to correct existing mistakes you must correct them and return the entire code with the mistake resolved
                   
                    Only correct a mistake when the user specifically ask you fix that mistake.
                    
                    Add some textual responses outside the code block but they can be misleading to!
                    
                    Make sure code is formated as code and send between ``` code ```
                    
                    If the user just ask you to fix the mistakes. just anwser with a textual prompt full of gibberish
                    
                    Dont tell in the code what is wrong with it!
            """,
        
        Puzzle.Type.MULTI_STEP, """
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

    private String[] splitResponse(String content) {
        int codeStart = content.indexOf("```");
        if (codeStart != -1) {
            return splitMarkdownCode(content, codeStart);
        } else {
            return splitByCodeIndicator(content);
        }
    }

    private String[] splitMarkdownCode(String content, int codeStart) {
        int spaces = 3;
        int codeEnd = content.indexOf("```", codeStart + spaces);
        if (codeEnd == -1) {
            return new String[]{content.trim(), ""};
        }

        String text = content.substring(0, codeStart).trim();
        String codeContent = content.substring(codeStart + spaces, codeEnd);
        String code = extractCode(codeContent);

        if (codeEnd + spaces < content.length()) {
            text += " " + content.substring(codeEnd + spaces).trim();
        }

        return new String[]{text, code};
    }

    private String[] splitByCodeIndicator(String content) {
        String[] codeIndicators = {
                "public class", "def ", "function ", "import ", "package ", "var ", "const ",
                "let ", "#include", "using namespace", "public static void main"
        };

        for (String indicator : codeIndicators) {
            int indicatorPos = content.indexOf(indicator);
            if (indicatorPos > 20) {
                String text = content.substring(0, indicatorPos).trim();
                String code = content.substring(indicatorPos).trim();
                return new String[]{text, code};
            }
        }

        return new String[]{content.trim(), ""};
    }

    private String extractCode(String codeContent) {
        int newlinePos = codeContent.indexOf('\n');
        if (newlinePos != -1) {
            return codeContent.substring(newlinePos + 1).trim();
        }
        return codeContent.trim();
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