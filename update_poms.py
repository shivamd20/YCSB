
import os

target_dir = "/Users/shivam.dwivedi/Documents/personal projects/shvm-db-benchmark/ycsb"

replacements = [
    ("<groupId>site.ycsb</groupId>", "<groupId>in.shvm.ycsb</groupId>"),
    # Handling version - be specific to avoid replacing random dependencies
    ("<version>0.18.0-SNAPSHOT</version>", "<version>0.18.0-shivam-SNAPSHOT</version>")
]

def update_poms():
    count = 0
    for root, dirs, files in os.walk(target_dir):
        for file in files:
            if file == "pom.xml":
                file_path = os.path.join(root, file)
                
                with open(file_path, 'r') as f:
                    content = f.read()
                
                original_content = content
                
                for old, new in replacements:
                    content = content.replace(old, new)
                
                if content != original_content:
                    print(f"Updating {file_path}")
                    with open(file_path, 'w') as f:
                        f.write(content)
                    count += 1
                        
    print(f"Updated {count} files.")

if __name__ == "__main__":
    update_poms()
