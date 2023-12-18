# kotlinx-io-supplementary-benchmarks

This repository contains benchmarks aimed to check if usage of direct `java.nio.ByteBuffer` for I/O
is beneficial compared to byte arrays. While the statement seems to be obvious and all the sources
on the Internet recommend using direct byte buffers whenever it's possible, there are almost no
articles or repositories proving that statement.

For the `kotlinx-io` we're [considering](https://github.com/Kotlin/kotlinx-io/issues/239) supporting
direct byte buffers as a backing storage for library's data container, so it's crucial to make sure
that byte buffers are worth it under assumption that `kotlinx-io`'s `Buffer`, `Source` and `Sink`
will retain its performance after switching to another backing storage (this assumption will be
validated separately).

There are various factors affecting overall application IO performance and these benchmarks are not
validating it. Instead, benchmarks are only measuring time required to read/receive or write/send
a prefilled byte array (using `java.io` streams) or direct byte buffer (using `java.nio` channels).

There are three group of benchmarks:
- benchmarks reading from and writing to UNIX virtual files `/dev/zero` and `/dev/null`;
- benchmarks reading from and writing to files in filesystem;
- benchmarks receiving data from and sending it to a socket connection.

Benchmarks using virtual files were added to check an extreme case where IO-stack don't have to
work with real block devices. These benchmarks should be more stable and, presumably, should depend
mostly on differences between `java.io` and `java.nio` read/write paths.

Raw benchmarking results are available in [results](results) directory.

**Android results**

The table below was generated for [results/2023-12-18T134000-android-SM-A145R-13.json](results/2023-12-18T134000-android-SM-A145R-13.json).

The speedup is calculated as average time ratio of byte-buffer based benchmarks to byte-array based benchmarks.
Ratio lower than 1 means that byte-buffer based benchmark faster, ratio higher than 1 means that byte-array based benchmark is faster.

|Buffer size, bytes|ByteArray-based benchmark|Average time, ns/op|0.999-CI, ns/op|ByteBuffer-based benchmark|Average time, ns/op|0.999-CI, ns/op|Speedup|
|------------------------|------------------|-------------------|---------------|--------------------------|------------------|-------------------|-------|
|8|c.e.b.VirtualFilesBenchmarks.inputStreamReadDevZero|3773.386|±61.152|c.e.b.VirtualFilesBenchmarks.fileChannelReadDevZero|1686.772|±0.712|0.447|
|128|c.e.b.VirtualFilesBenchmarks.inputStreamReadDevZero|3869.382|±53.221|c.e.b.VirtualFilesBenchmarks.fileChannelReadDevZero|1696.630|±0.715|0.438|
|1,024|c.e.b.VirtualFilesBenchmarks.inputStreamReadDevZero|4114.704|±40.706|c.e.b.VirtualFilesBenchmarks.fileChannelReadDevZero|1801.493|±0.761|0.437|
|8,192|c.e.b.VirtualFilesBenchmarks.inputStreamReadDevZero|6704.139|±50.325|c.e.b.VirtualFilesBenchmarks.fileChannelReadDevZero|2735.855|±1.247|0.408|
|8|c.e.b.VirtualFilesBenchmarks.outputStreamWriteDevNull|4168.081|±346.301|c.e.b.VirtualFilesBenchmarks.fileChannelWriteDevNull|1977.860|±1.633|0.474|
|128|c.e.b.VirtualFilesBenchmarks.outputStreamWriteDevNull|4479.640|±54.961|c.e.b.VirtualFilesBenchmarks.fileChannelWriteDevNull|1957.978|±0.909|0.437|
|1,024|c.e.b.VirtualFilesBenchmarks.outputStreamWriteDevNull|4533.645|±54.566|c.e.b.VirtualFilesBenchmarks.fileChannelWriteDevNull|1963.666|±0.894|0.433|
|8,192|c.e.b.VirtualFilesBenchmarks.outputStreamWriteDevNull|5386.453|±75.642|c.e.b.VirtualFilesBenchmarks.fileChannelWriteDevNull|1957.937|±0.828|0.363|
|8|c.e.b.FilesBenchmarks.fileInputStream|4015.961|±76.995|c.e.b.FilesBenchmarks.fileChannelRead|1750.668|±7.555|0.435|
|128|c.e.b.FilesBenchmarks.fileInputStream|4193.847|±68.058|c.e.b.FilesBenchmarks.fileChannelRead|1825.418|±1.430|0.435|
|1,024|c.e.b.FilesBenchmarks.fileInputStream|4932.476|±70.408|c.e.b.FilesBenchmarks.fileChannelRead|2330.664|±16.985|0.472|
|8,192|c.e.b.FilesBenchmarks.fileInputStream|10591.754|±72.919|c.e.b.FilesBenchmarks.fileChannelRead|6095.612|±68.536|0.575|
|8|c.e.b.FilesBenchmarks.fileOutputStream|6496.406|±263.772|c.e.b.FilesBenchmarks.fileChannelWrite|3188.997|±32.938|0.490|
|128|c.e.b.FilesBenchmarks.fileOutputStream|7021.836|±244.318|c.e.b.FilesBenchmarks.fileChannelWrite|3431.759|±58.971|0.488|
|1,024|c.e.b.FilesBenchmarks.fileOutputStream|8608.663|±226.394|c.e.b.FilesBenchmarks.fileChannelWrite|3560.844|±17.192|0.413|
|8,192|c.e.b.FilesBenchmarks.fileOutputStream|18458.610|±350.824|c.e.b.FilesBenchmarks.fileChannelWrite|5263.322|±55.681|0.285|
|8|c.e.b.SocketReadBenchmarks.inputStream|5981.188|±67.039|c.e.b.SocketReadBenchmarks.channel|2077.462|±27.062|0.347|
|128|c.e.b.SocketReadBenchmarks.inputStream|6492.154|±62.013|c.e.b.SocketReadBenchmarks.channel|2172.125|±10.489|0.334|
|1,024|c.e.b.SocketReadBenchmarks.inputStream|8015.609|±76.097|c.e.b.SocketReadBenchmarks.channel|2889.347|±12.995|0.360|
|8,192|c.e.b.SocketReadBenchmarks.inputStream|12167.877|±102.898|c.e.b.SocketReadBenchmarks.channel|10007.487|±706.403|0.822|
|8|c.e.b.SocketWriteBenchmarks.outputStream|8749.613|±125.335|c.e.b.SocketWriteBenchmarks.channel|3109.160|±101.096|0.355|
|128|c.e.b.SocketWriteBenchmarks.outputStream|8984.040|±162.009|c.e.b.SocketWriteBenchmarks.channel|3006.751|±101.727|0.334|
|1,024|c.e.b.SocketWriteBenchmarks.outputStream|9740.933|±199.402|c.e.b.SocketWriteBenchmarks.channel|3981.524|±114.510|0.408|
|8,192|c.e.b.SocketWriteBenchmarks.outputStream|14039.597|±131.971|c.e.b.SocketWriteBenchmarks.channel|7287.122|±144.416|0.519|

**JVM results**

The table below was generated for [results/2023-12-18T134000-android-SM-A145R-13.json](results/2023-12-18T134000-android-SM-A145R-13.json).

The speedup is calculated as average time ratio of byte-buffer based benchmarks to byte-array based benchmarks.
Ratio lower than 1 means that byte-buffer based benchmark faster, ratio higher than 1 means that byte-array based benchmark is faster.

`N/A` in speedup column occurs when CI for two benchmarks overlap, meaning that difference between two averages may** be statistically insignificant (given the .999 CI).

** or may be still significant, but we should use a better approach to check it.

|Buffer size, bytes|ByteArray-based benchmark|Average time, ns/op|0.999-CI, ns/op|ByteBuffer-based benchmark|Average time, ns/op|0.999-CI, ns/op|Speedup|
|------------------------|------------------|-------------------|---------------|--------------------------|------------------|-------------------|-------|
|8|o.e.FileInputStreamReadFromDevZero.b|434.761651|±12.327|o.e.FileChannelReadFromDevZero.b|419.773|±8.539|N/A|
|128|o.e.FileInputStreamReadFromDevZero.b|464.540388|±17.038|o.e.FileChannelReadFromDevZero.b|430.508|±21.518|N/A|
|1024|o.e.FileInputStreamReadFromDevZero.b|479.628879|±9.633|o.e.FileChannelReadFromDevZero.b|445.044|±11.855|0.928|
|8192|o.e.FileInputStreamReadFromDevZero.b|695.258074|±9.960|o.e.FileChannelReadFromDevZero.b|585.442|±1.484|0.842|
|8|o.e.FileOutputStreamWriteToDevNull.b|427.341563|±23.610|o.e.FileChannelWriteToDevNull.b|373.672|±4.955|0.874|
|128|o.e.FileOutputStreamWriteToDevNull.b|421.559634|±1.326|o.e.FileChannelWriteToDevNull.b|373.901|±9.284|0.887|
|1024|o.e.FileOutputStreamWriteToDevNull.b|423.879188|±0.873|o.e.FileChannelWriteToDevNull.b|372.180|±7.128|0.878|
|8192|o.e.FileOutputStreamWriteToDevNull.b|544.751738|±9.478|o.e.FileChannelWriteToDevNull.b|370.058|±7.731|0.679|
|8|o.e.FileInputStreamBenchmark.b|768.221477|±9.863|o.e.FileChannelReadBenchmark.b|733.899|±19.321|0.955|
|128|o.e.FileInputStreamBenchmark.b|774.864774|±6.706|o.e.FileChannelReadBenchmark.b|741.560|±7.309|0.957|
|1024|o.e.FileInputStreamBenchmark.b|832.457618|±6.111|o.e.FileChannelReadBenchmark.b|770.116|±12.333|0.925|
|8192|o.e.FileInputStreamBenchmark.b|1555.881413|±14.063|o.e.FileChannelReadBenchmark.b|1265.311|±23.348|0.813|
|8|o.e.FileOutputStreamBenchmark.b|2415.642710|±8.221|o.e.FileChannelWriteBenchmark.b|1111.296|±45.123|0.460|
|128|o.e.FileOutputStreamBenchmark.b|2581.291673|±13.172|o.e.FileChannelWriteBenchmark.b|1100.616|±5.480|0.426|
|1024|o.e.FileOutputStreamBenchmark.b|3863.201074|±40.177|o.e.FileChannelWriteBenchmark.b|1174.607|±26.775|0.304|
|8192|o.e.FileOutputStreamBenchmark.b|19844.545290|±59.666|o.e.FileChannelWriteBenchmark.b|2176.827|±165.007|0.110|
|8|o.e.SocketInputStreamBenchmark.b|1004.442341|±26.231|o.e.SocketChannelReadBenchmark.b|979.546|±15.658|N/A|
|128|o.e.SocketInputStreamBenchmark.b|1046.511236|±9.962|o.e.SocketChannelReadBenchmark.b|998.975|±11.193|0.955|
|1024|o.e.SocketInputStreamBenchmark.b|1179.175931|±8.014|o.e.SocketChannelReadBenchmark.b|1129.367|±13.140|0.958|
|8192|o.e.SocketInputStreamBenchmark.b|3468.386942|±1539.722|o.e.SocketChannelReadBenchmark.b|4366.602|±767.332|N/A|
|8|o.e.SocketOutputStreamBenchmark.b|1899.676482|±45.576|o.e.SocketChannelWriteBenchmark.b|1807.642|±27.942|0.952|
|128|o.e.SocketOutputStreamBenchmark.b|1142.531536|±10.716|o.e.SocketChannelWriteBenchmark.b|1126.650|±11.014|N/A|
|1024|o.e.SocketOutputStreamBenchmark.b|1555.579764|±19.441|o.e.SocketChannelWriteBenchmark.b|1530.851|±14.670|N/A|
|8192|o.e.SocketOutputStreamBenchmark.b|2460.177158|±7.744|o.e.SocketChannelWriteBenchmark.b|2454.384|±9.458|N/A|
