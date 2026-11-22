REPO: vaadin/flow-components
TITLE: A failed AI turn writes a fixed English sentence the application cannot replace
---
### Description

When a turn fails, `AIOrchestrator` writes `An error occurred. Please try again.` into the message list, and `Request timed out. Please try again.` on a timeout. Both are constants in `AIOrchestrator`; there is no builder method, i18n object or hook to change them. User name and assistant name can be set with `withUserName` and `withAssistantName`, so a Finnish chat reads `Avustaja: An error occurred. Please try again.`

The 25.4 `ToolCallLimitExceededException` makes this sharper: the turn that hit the limit says the same generic sentence, and the exception carries which limit was exceeded and how many calls were made only inside its message, worded differently per provider, with no accessor.

### Why it matters

Every application that is not in English, or that has its own voice, has to replace the sentence after the fact: find the last item of the list in a response listener and overwrite it. An application that wants to say "gave up after 40 tool calls" has to count the calls itself.

### Expected

1. The two sentences in an i18n object on the orchestrator, or a `withErrorMessage(Function<Throwable, String>)` on the builder, so the message can depend on the failure.
2. `ToolCallLimitExceededException` with accessors for the limit, the count and the tool, when it was a per tool limit.

### Workaround

The response listener replaces the text of the last item of the `MessageList` with a sentence from the translation bundle, and the application's own `AIController` counts the tool calls of the turn so the limit sentence can name them.

### Reproduce

`mvn test -Dtest=FailureTextTest` in the project below. A chat with a Finnish UI locale and Finnish names, whose provider fails with `ToolCallLimitExceededException`, prints:

```
Sinä: Montako?
Avustaja: An error occurred. Please try again.
reported: com.vaadin.flow.component.ai.provider.ToolCallLimitExceededException: Tool call limit exceeded: lookup was called 41 times
public methods of the exception beyond Throwable: []
```

### Getting the project

[`21-ai-surfaces.zip`](https://github.com/vaadin/hackathon-25-4/raw/COMMIT_SHA/specs/issues/projects/21-ai-surfaces.zip), 5 KB, sources only. The test of this draft needs no licence and no key. Unzip it and:

```
mvn test -Dtest=FailureTextTest
```

Found on 25.4.0-beta2. The sentence is the same on 25.3.5; the exception is new in 25.4.
