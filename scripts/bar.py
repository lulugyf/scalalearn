
import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import MaxNLocator
from collections import namedtuple


n_groups = 3
means_men = (9171, 9262, 10349)
#std_men = (2, 3, 4, 1, 2)

means_women = (2973, 3476, 4447)
#std_women = (3, 5, 2, 3, 3)

fig, ax = plt.subplots()

index = np.arange(n_groups)
bar_width = 0.35

opacity = 0.4
error_config = {'ecolor': '0.3'}

rects1 = ax.bar(index, means_men, bar_width,
                alpha=opacity, color='b',
                #yerr=std_men, error_kw=error_config,
                label='Tomcat-JDBC')

rects2 = ax.bar(index + bar_width, means_women, bar_width,
                alpha=opacity, color='r',
                #yerr=std_women, error_kw=error_config,
                label='C3P0Pool')

ax.set_xlabel('Thread count')
ax.set_ylabel('Time(ms) of 1000,000')
ax.set_title('C3P0Pool VS. Tomcat-JDBC')
ax.set_xticks(index + bar_width / 2)
ax.set_xticklabels(('10', '20', '40'))
ax.legend()

fig.tight_layout()
plt.show()