# Repo contract (nextjedi/platform docs/CONTRACTS.md): setup, dev, verify, e2e.
# `make verify` is what CI and the Claude Stop hook run. It must stay green.
GRADLEW ?= sh ./gradlew
GRADLE_FLAGS ?= --console=plain
APP_ID := com.ashutosh.flowtimer

.DEFAULT_GOAL := help
.PHONY: help setup dev verify lint test build coverage e2e format baseline clean

help: ## List targets
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "  %-15s %s\n", $$1, $$2}'

setup: ## Install git hooks, create .env from .env.example and warm the Gradle wrapper
	@if command -v lefthook >/dev/null 2>&1; then lefthook install; else echo "lefthook not installed: skipping git hooks (npm i -g lefthook / winget install evilmartians.lefthook)"; fi
	@test -f .env || cp .env.example .env
	$(GRADLEW) $(GRADLE_FLAGS) help

dev: ## Build, install and launch the phone app on the connected device or emulator
	$(GRADLEW) $(GRADLE_FLAGS) :app:installDebug
	adb shell am start -n $(APP_ID)/.MainActivity

verify: ## ktlint, detekt, unit tests + coverage, Android lint, compile androidTest, assemble app + wear (debug)
	$(GRADLEW) $(GRADLE_FLAGS) ktlintCheck detekt \
		:app:testDebugUnitTest :app:koverXmlReportDebug \
		:app:lintDebug :wear:lintDebug \
		:app:assembleDebug :app:assembleDebugAndroidTest :wear:assembleDebug

lint: ## ktlint + detekt + Android lint only
	$(GRADLEW) $(GRADLE_FLAGS) ktlintCheck detekt :app:lintDebug :wear:lintDebug

test: ## Unit tests (JVM) with Kover coverage
	$(GRADLEW) $(GRADLE_FLAGS) :app:testDebugUnitTest :app:koverXmlReportDebug

build: ## Debug APKs for app and wear
	$(GRADLEW) $(GRADLE_FLAGS) :app:assembleDebug :wear:assembleDebug

e2e: ## Instrumented tests on a connected device/emulator (skips when none is attached)
	@if adb get-state >/dev/null 2>&1; then $(GRADLEW) $(GRADLE_FLAGS) :app:connectedDebugAndroidTest; else echo "e2e: no Android device or emulator attached, skipping"; fi

format: ## Auto-fix Kotlin formatting
	$(GRADLEW) $(GRADLE_FLAGS) ktlintFormat

baseline: ## Re-record detekt and wear lint baselines (only when deliberately accepting existing findings)
	$(GRADLEW) $(GRADLE_FLAGS) detektBaseline :wear:updateLintBaseline

clean: ## Remove build outputs
	$(GRADLEW) $(GRADLE_FLAGS) clean
