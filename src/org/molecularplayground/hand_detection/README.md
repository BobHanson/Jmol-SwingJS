# Molecular Playground Local Python Server

- Adapted from Victor Dibia, Real-time Hand-Detection using Neural Networks (SSD) on Tensorflow, (2017), GitHub repository, https://github.com/victordibia/handtracking

### To Run

> Make sure that you have python >3.6 installed ([install here], as well as Java.(https://www.python.org/downloads/))

1.  Clone the repository

`git clone <url>`

2. Run the detection script (Note: this should take several seconds, as the NN graph is loaded and the workers begin)


`python detect_mult_threaded.py`

1. Run the local JMOL molecule.

`java -jar MPJmolApp.jar`

---
![motion](assets/motion.gif)

---

*Current Output:*

```(javascript)
{'magic': 'JmolApp', 'role': 'out'}
{"type": "move", "style": "rotate", "x": 1, "y": 10}
{"type": "move", "style": "rotate", "x": 18, "y": 12}
{"type": "move", "style": "rotate", "x": 5, "y": 7}
{"type": "move", "style": "rotate", "x": 7, "y": 4}
{"type": "move", "style": "rotate", "x": 10, "y": 8}
{"type": "move", "style": "rotate", "x": 11, "y": 4}
{"type": "move", "style": "rotate", "x": 11, "y": 2}
{"type": "move", "style": "rotate", "x": 14, "y": 4}
{"type": "move", "style": "rotate", "x": 3, "y": 3}
{"type": "move", "style": "rotate", "x": 14, "y": 2}
{"type": "move", "style": "rotate", "x": 9, "y": 2}
{"type": "move", "style": "rotate", "x": 18, "y": 2}
...
...
```