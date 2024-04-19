import pandas as pd
import matplotlib.pyplot as plt

# Read data from CSV file into a DataFrame
df = pd.read_csv('/content/Data.csv')

# Define colors for each lock type
colors = {'FilterLock': 'blue', 'FilterLockWithGPL': 'green', 'BakeryLock': 'red', 'TournamentTree': 'orange'}

# Plotting
fig, ax1 = plt.subplots(figsize=(10, 6))

# Plot throughput
ax1.set_xlabel('Number of Threads')
ax1.set_ylabel('Throughput', color='black')
for locktype, group in df.groupby('locktype'):
    ax1.plot(group['numThreads'], group['Throughput'], label=locktype, color=colors.get(locktype, 'black'))
ax1.tick_params(axis='y', labelcolor='black')

# Create a second y-axis for time elapsed
ax2 = ax1.twinx()
ax2.set_ylabel('Time Elapsed (s)', color='grey')
for locktype, group in df.groupby('locktype'):
    ax2.plot(group['numThreads'], group['ElapsedTime(s)'], linestyle='--', label=f'{locktype} (Time)', color=colors.get(locktype, 'grey'))
ax2.tick_params(axis='y', labelcolor='grey')

# Common title and legend
fig.suptitle('Throughput and Time Elapsed vs. Number of Threads for Different Lock Types')
fig.legend(loc='upper right', bbox_to_anchor=(1,1), bbox_transform=ax1.transAxes)

# Show plot
plt.grid(True)
plt.tight_layout()
plt.show()