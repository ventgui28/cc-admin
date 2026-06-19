with open("app/src/main/java/com/ventgui/app/ui/screens/ReportsScreen.kt", "r", encoding="utf-8") as f:
    for i, line in enumerate(f, 1):
        if "@Composable" in line or "fun " in line:
            if "fun " in line:
                print(f"{i}: {line.strip()}")
