#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# TextingStory Clone - Termux Deploy Script
# Run this ONCE after unzipping the project.
# Usage: bash setup.sh YOUR_GITHUB_PAT
# ============================================================

set -e

PAT="${1:-}"
USERNAME="username051203"
EMAIL="lewin.nick19@gmail.com"
REPO_NAME="textingstory-clone"
PROJECT_DIR="$HOME/TextingStoryClone"

if [ -z "$PAT" ]; then
  echo "Usage: bash setup.sh YOUR_GITHUB_PAT"
  exit 1
fi

echo "=== Checking dependencies ==="
pkg install -y git openjdk-17 2>/dev/null || true

echo "=== Downloading gradle-wrapper.jar ==="
mkdir -p "$PROJECT_DIR/gradle/wrapper"
curl -L "https://github.com/nicehash/nicehashminer/raw/master/gradle/wrapper/gradle-wrapper.jar" \
  -o "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null || \
curl -L "https://services.gradle.org/distributions/gradle-8.6-wrapper.jar" \
  -o "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" 2>/dev/null || \
  echo "WARNING: Could not download wrapper jar. You may need to do this manually."

# Better approach: use gradle itself to generate wrapper
if ! [ -f "$PROJECT_DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  echo "Trying gradle wrapper via SDK..."
fi

echo "=== Git setup ==="
cd "$PROJECT_DIR"
git config --global user.name "$USERNAME"
git config --global user.email "$EMAIL"
git init
git add -A
git commit -m "Initial commit: TextingStory clone v1.0"

echo "=== Creating GitHub repo ==="
curl -s -X POST "https://api.github.com/user/repos" \
  -H "Authorization: token $PAT" \
  -H "Content-Type: application/json" \
  -d "{\"name\":\"$REPO_NAME\",\"private\":false,\"description\":\"TextingStory clone - Android, Room DB, no limits\"}"

echo ""
echo "=== Pushing to GitHub ==="
git remote add origin "https://$USERNAME:$PAT@github.com/$USERNAME/$REPO_NAME.git"
git branch -M main
git push -u origin main

echo ""
echo "=== DONE! ==="
echo "Repo: https://github.com/$USERNAME/$REPO_NAME"
echo "GitHub Actions will now build your APK automatically."
echo ""
echo "To tag a release and trigger APK publish:"
echo "  git tag v1.0.0 && git push origin v1.0.0"
