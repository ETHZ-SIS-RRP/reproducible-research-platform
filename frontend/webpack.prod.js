const path = require('path')
const HtmlWebpackPlugin = require('html-webpack-plugin')

module.exports = env => ({
  entry: './index.ts',
  mode: 'production',
  plugins: [
    new HtmlWebpackPlugin({
      template: 'index.html',
      favicon: 'favicon.ico',
      inject: 'body'
    })
  ],
  output: {
    path: env.BUILD_DIR,
    filename: 'rrp.[fullhash].bundle.js',
    publicPath: '/'
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
})
