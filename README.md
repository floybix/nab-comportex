# nab-comportex

Support and experiments for running
[Comportex](https://github.com/htm-community/comportex) HTM models on
the [Numenta Anomaly Benchmark](https://github.com/numenta/NAB).

For context, read the blog post
[Attempting the Numenta Anomaly Benchmark](http://floybix.github.io/2016/07/01/attempting-nab).

## Results

The results as submitted to the
[2016 NAB Competition](http://numenta.org/nab/) can be found in the
`results` directory.

Headline results:

| model                                         | standard | low FP rate | low FN rate |
|-----------------------------------------------|----------|-------------|-------------|
| _original NuPIC model + anomaly likelihood_   | 65.3     | 58.6        | 69.4        |
| _original NuPIC model, raw bursting score_    | 52.5     | 41.1        | 58.3        |
| selected Comportex model, delta anomaly score | 64.6     | 58.8        | 69.6        |

Effective time step model results:

| model                                         | standard | low FP rate | low FN rate |
|-----------------------------------------------|----------|-------------|-------------|
| effective time steps when 20% columns change  | 64.7     | 59.8        | 69.5        |


## Usage

To reproduce the results submitted to the
[2016 NAB Competition](http://numenta.org/nab/):

1. install [Leiningen](http://leiningen.org/).
2. clone this git repo. It should be placed alongside the `NAB`
   directory so that data files can be read from and result files
   written to `../NAB`.
  * `git checkout v0.1.0`
3. In the `NAB` directory:
  * `python scripts/create_new_detector.py --detector comportexDepth1GlobalFrac16Stim18`
  * `python scripts/create_new_detector.py --detector comportexDepth1GlobalFrac16Stim18NoDelta`
  * `python scripts/create_new_detector.py --detector comportexDepth1EfftGlobalFrac16Stim18`
4. In the `nab-comportex` directory:
  * `lein repl`
5. In the repl: `(start-notebook)`
6. Note the port number such as 12345, then go to
   http://localhost:12345/worksheet.html?filename=worksheeets/method.clj
7. Keep pressing Shift+Enter...

The results will be written into `../NAB/results/`.

It would also be possible to run the clojure file
`worksheets/method.clj` in other ways, interactive or not.


## License

Copyright © 2016 Felix Andrews

Distributed under the
[GNU Affero General Public Licence, Version 3](http://www.gnu.org/licenses/agpl-3.0.en.html),
the same as Comportex.
