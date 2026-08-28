# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A lightweight Java library (`io.github.surajs1n:email-normalization`) that validates and normalizes morphed email addresses to a canonical form (e.g. `Suraj.Sn+123@gmail.com` -> `surajsn@gmail.com`). It exists to stop users from creating multiple accounts / abusing OTP flows via cosmetic variations (dots, `+suffix`, casing) of the same email address.

## Build & test commands

```bash
mvn clean package         # build (CI runs this exact command)
mvn test                  # run all tests
mvn test -Dtest=EmailNormalizerTest                       # run a single test class
mvn test -Dtest=EmailNormalizerTest#GIVEN_gmailIdWithDot_WHEN_Normalizing_THEN_NormalizeIt  # run a single test method
```

Requires JDK 18 (`maven.compiler.source/target = 18`) and Lombok (annotation processing must be enabled if using an IDE). CI (`.github/workflows/maven.yml`) runs `mvn clean -B package` on pushes/PRs to `develop`.

## Architecture

Entry point: `normalize.EmailNormalizer` (singleton via `getInstance()`, or `getInstance(yamlFilePath)` for a custom-config singleton — note both singletons are cached statically, which matters when writing tests that need different configs in the same JVM run).

Flow of `normalize(String)`:
1. Validate the raw string with Apache Commons `EmailValidator`; invalid input throws `EmailNormalizationException(INVALID_EMAIL)`.
2. Split the address into a `model.Email` (`emailPrefix`, `emailProvider`, `topLevelDomain`) — split on the first `@`, then split the remainder on the first `.` into provider vs. TLD.
3. `factory.EmailHandlerFactoryImpl.normalizeEmail(email)`:
   - Maps the provider string to a `model.enums.EmailProviderType` via `util.EmailUtils.getEmailProviderType` (handles aliases, e.g. `googlemail` -> `GMAIL`, `yahoomail` -> `YAHOO`).
   - Looks up the ordered `List<EmailHandler>` for that provider type from `EmailConfiguration` (falls back to the `OTHER` entry if the provider has no configured handlers).
   - Runs each handler's `handle(email)` in order, mutating the `Email` object in place.
4. Reassembles `emailPrefix@emailProvider.topLevelDomain` as the result.

### Handler chain (Strategy pattern, config-driven)

`factory.handler.EmailHandler` is the abstract base; concrete handlers are Jackson polymorphic subtypes keyed by `handlerType` (`@JsonTypeInfo`/`@JsonSubTypes` in `EmailHandler.java`):
- `TO_LOWERCASE` (`ToLowerEmailHandler`) — lowercases prefix, provider, and TLD.
- `REMOVE_CHARACTERS` (`RemoveCharacterFromEmailHandler`) — regex-strips characters from the prefix (`patternOfCharacterToBeRemoved`).
- `TRIM_SUFFIX` (`TrimFromEmailHandler`) — truncates the prefix at the first match of `trimRegexExpression` (e.g. `\+` for `+`-suffix tagging).

Adding a new handler type means: add an enum value to `EmailHandlerType`, create the class extending `EmailHandler`, register it in the `@JsonSubTypes` list on `EmailHandler`, and reference it by name from YAML config.

### Config loading (`reader.YAMLConfigReader`, `config.EmailConfiguration`, `util.EmailUtils`)

- The built-in default config is a hardcoded `Map<EmailProviderType, List<EmailHandler>>` in `EmailUtils` (not read from `EmailConfigurations.yaml` at runtime — that YAML file exists for documentation/reference purposes and mirrors the hardcoded default). Default: GMAIL gets lowercase + dot-removal + `+`-suffix trim; every other known provider gets lowercase-only.
- A **custom** config (`EmailNormalizer.getInstance(path)`) is merged *on top of* the default map — `CUSTOM_EMAIL_CONFIGURATION.getEmailProviderTypeListMap().putAll(customFromFile)` — so a custom YAML only needs to specify the providers it wants to override; unspecified providers keep the built-in default handlers.
- `YAMLConfigReader` and both `EmailNormalizer` instances are lazily-initialized singletons cached in static fields. There's exactly one default instance and one custom instance per JVM (the custom one is pinned to whichever `yamlFilePath` was passed the *first* time `getInstance(path)` is called — later calls with a different path are ignored). Keep this in mind when writing tests that exercise multiple custom configs.

### Provider matching

`EmailProviderType` enum: `GMAIL, OUTLOOK, AOL, YAHOO, ICLOUD, ZOHO, YANDEX, TUTANOTA, OTHER`. Matching is by exact provider-string alias lookup in `EmailUtils.nameToEmailProviderType` (case-insensitive) — there's no TLD or domain-correctness validation (e.g. `gmail.co.in` and `gmail.com` are both treated as `GMAIL`; a typo'd provider like `gnail.com` falls through to `OTHER`).

## Writing a custom YAML config

See README.md "Write your own custom config file" section for the full spec. Key points to remember when editing/testing configs:
- Top-level key is always `emailProviderTypeListMap`.
- Each provider maps to an ordered list of `{handlerType, ...params}` entries; handlers run in list order.
- Overriding a provider replaces its entire handler list (not a merge at the handler level) — only unlisted providers inherit the default.
- `src/main/resources/config/CustomEmailConfiguration.yaml` is a working example used by `CustomEmailNormalizerTest`.
