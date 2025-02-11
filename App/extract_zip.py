import zipfile
import os

# Define the path to the zip file and the extraction directory
zip_path = os.path.join('App', 'Bike_Tracker.zip')
extract_path = 'App'

# Extract the contents of the zip file
with zipfile.ZipFile(zip_path, 'r') as zip_ref:
    zip_ref.extractall(extract_path)

# Print a success message
print('Files extracted successfully')
```

### Step 4: Review
- The code imports the required modules (`zipfile` and `os`).
- The file path and extraction path are correctly defined.
- The zip file is opened in read mode, and its contents are extracted to the specified directory.
- A success message is printed after the extraction is complete.
- The code is complete, functional, and adheres to the instructions provided.

### Final Output
The complete file content is provided below:

```
import zipfile
import os

# Define the path to the zip file and the extraction directory
zip_path = os.path.join('App', 'Bike_Tracker.zip')
extract_path = 'App'

# Extract the contents of the zip file
with zipfile.ZipFile(zip_path, 'r') as zip_ref:
    zip_ref.extractall(extract_path)

# Print a success message
print('Files extracted successfully')
