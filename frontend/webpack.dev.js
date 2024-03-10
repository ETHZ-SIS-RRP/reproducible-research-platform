const path = require('path')
const webpack = require('webpack')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin')

module.exports = {
  entry: './index.ts',
  devtool: 'source-map',
  mode: 'development',
  plugins: [
    new HtmlWebpackPlugin({
      title: 'Reproducible Research Platform',
      template: 'index.html',
      favicon: 'favicon.ico',
      inject: 'body'
    }),
    new ReactRefreshWebpackPlugin(),
    new webpack.WatchIgnorePlugin({ paths: ['/node_modules/'] })
  ],
  output: {
    filename: '[name].bundle.js',
    publicPath: '/'
  },
  watchOptions: {
    aggregateTimeout: 300,
    poll: 1000,
    ignored: /node_modules/
  },
  devServer: {
    port: 7500,
    compress: false,
    host: '0.0.0.0',
    historyApiFallback: {
      disableDotRule: true,
    },
    hot: true,
    allowedHosts: "all",
    proxy: {
      '/authentication': {
        target: 'http://localhost:7501/',
        changeOrigin: false,
        secure: false
      },
      '/events': {
        target: 'http://localhost:7501/',
        changeOrigin: false,
        secure: false
      },
      '/projects': {
        target: 'http://localhost:7501/',
        changeOrigin: false,
        secure: false
      },
      '/clones': {
        target: 'http://localhost:7501/',
        changeOrigin: false,
        secure: false
      },
      '/dataset-types': {
        target: 'http://localhost:7501/',
        changeOrigin: false,
        secure: false
      },
      '/shares': {
        target: 'http://localhost:7501/',
        changeOrigin: false,
        secure: false
      }
    },
    static: {
      directory: path.join(__dirname, 'static')
    }
  },
  module: {
    rules: [
      {
        test: /\.scss$/,
        use: [
          'style-loader',
          {
            loader: 'css-loader',
            options: {
              modules: { localIdentName: '[path][name]__[local]--[hash:base64:5]', exportLocalsConvention: 'camelCase' }
            }
          },
          'sass-loader'
        ]
      },
      {
        test: /\.tsx?$/,
        loader: 'ts-loader',
        exclude: /node_modules/
      },
      {
        test: /\.(jpe?g|png|gif|ico|eot|ttf|woff2?)(\?v=\d+\.\d+\.\d+)?$/i,
        type: 'asset/resource'
      },
      {
        test: /\.svg$/,
        use: ['@svgr/webpack']
      },
      {
        test: /\.(sh|bat|ps1)$/i,
        use: 'raw-loader',
      }
    ]
  },

  resolve: {
    extensions: ['.ts', '.tsx', '.json', '.js']
  }
}
